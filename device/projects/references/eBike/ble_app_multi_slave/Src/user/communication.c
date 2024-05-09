#include "communication.h"
#include <string.h>


#define CMD_FRAME_HEADER_L      0x7f
#define CMD_FRAME_HEADER_H      0xf7
#define ACK_SUCCESS             0x01
#define ACK_ERROR               0x02
#define PATTERN_VALUE           (0xf77f)

typedef enum
{
    CHECK_FRAME_L_STATE = 0x00,
    CHECK_FRAME_H_STATE,
    RECEIVE_CMD_TYPE_L_STATE,
    RECEIVE_CMD_TYPE_H_STATE,
    RECEIVE_LEN_L_STATE,
    RECEIVE_LEN_H_STATE,
    RECEIVE_DATA_STATE,
    RECEIVE_CHECK_SUM_L_STATE,
    RECEIVE_CHECK_SUM_H_STATE,
} cmd_parse_state_t;


static receive_frame_t   s_receive_frame;
static bool              s_cmd_receive_flag;
static uint16_t          s_receive_data_count;
static uint32_t          s_receive_check_sum;
static cm_func_cfg_t *s_p_func_cfg;

static uint16_t          s_sended_len;
static uint16_t          s_all_send_len;
static uint16_t          s_once_size = 244;
static uint8_t*          s_send_data_buffer;

static cmd_parse_state_t s_parse_state = CHECK_FRAME_L_STATE;


static void cm_send_data(uint8_t *data, uint16_t len)
{
    if(s_p_func_cfg -> cm_send_data != NULL)
    {
        s_p_func_cfg -> cm_send_data(data, len);
    }
}

static void cm_event_handler(uint8_t status, receive_frame_t *p_frame)
{
    if(s_p_func_cfg -> cm_event_handler != NULL)
    {
        s_p_func_cfg -> cm_event_handler(status, p_frame);
    }
}

static void cm_cmd_check(void)
{
    uint16_t i = 0;
    for(i=0; i<s_receive_frame.data_len; i++)
    {
        s_receive_check_sum += s_receive_frame.data[i];
    }
    
    if((s_receive_check_sum & 0xffff) == s_receive_frame.check_sum)
    {
        s_cmd_receive_flag = true;
    }
    else
    {
        s_cmd_receive_flag = false;
    }
}

static void cm_send(uint8_t *data, uint16_t len)
{
    s_send_data_buffer = s_receive_frame.data;
    memcpy(s_send_data_buffer,data,len);
    s_all_send_len = len;
    if(len >= s_once_size)
    {
        s_sended_len = s_once_size;
    }
    else
    {
        s_sended_len = len;
    }

    cm_send_data(s_send_data_buffer,s_sended_len);
}

void cm_send_frame(uint8_t *data,uint16_t len,uint16_t cmd_type)
{
    uint8_t send_data[RECEIVE_MAX_LEN + 8];
    uint16_t i = 0;
    uint32_t check_sum = 0;
    send_data[0] = CMD_FRAME_HEADER_L;
    send_data[1] = CMD_FRAME_HEADER_H;
    send_data[2] = cmd_type;
    send_data[3] = cmd_type >> 8;
    send_data[4] = len;
    send_data[5] = len >> 8;
    
    for(i=2; i<6; i++)
    {
        check_sum += send_data[i];
    }
    
    for(i=0; i<len; i++)
    {
        send_data[6+i] = *(data+i);
        check_sum += *(data+i);
    }
    send_data[6+len] = check_sum;
    send_data[7+len] = check_sum >> 8;
    cm_send(send_data,len+8);
}

void cm_send_data_cmpl_process(void)
{
    int remain = s_all_send_len - s_sended_len;

    if(remain >= s_once_size)
    {
        cm_send_data(&s_send_data_buffer[s_sended_len], s_once_size);
        s_sended_len += s_once_size;
    }
    else if(remain > 0)
    {
        cm_send_data(&s_send_data_buffer[s_sended_len],remain);
        s_sended_len += remain;
    }
}

void cm_cmd_prase(uint8_t* data,uint16_t len)
{
    uint16_t i = 0;
    
    if(s_cmd_receive_flag == 0)
    {
        for(i=0; i<len; i++)
        {
            switch(s_parse_state)
            {
                case CHECK_FRAME_L_STATE:
                {
                    s_receive_check_sum = 0;
                    if(data[i] == CMD_FRAME_HEADER_L)
                    {
                        s_parse_state = CHECK_FRAME_H_STATE;
                    }
                }
                break;
                
                case CHECK_FRAME_H_STATE:
                {
                    if(data[i] == CMD_FRAME_HEADER_H)
                    {
                        s_parse_state = RECEIVE_CMD_TYPE_L_STATE;
                    } else if(data[i] == CMD_FRAME_HEADER_L) {
                        s_parse_state = CHECK_FRAME_H_STATE;
                    } else {
                        s_parse_state = CHECK_FRAME_L_STATE;
                    }
                }
                break;
                
                case RECEIVE_CMD_TYPE_L_STATE:
                {
                    s_receive_frame.cmd_type = data[i];
                    s_receive_check_sum += data[i];
                    s_parse_state = RECEIVE_CMD_TYPE_H_STATE;
                }
                break;
                
                case RECEIVE_CMD_TYPE_H_STATE:
                {
                    s_receive_frame.cmd_type |= (data[i] << 8);
                    s_receive_check_sum += data[i];
                    s_parse_state = RECEIVE_LEN_L_STATE;
                }
                break;
                
                case RECEIVE_LEN_L_STATE:
                {
                    s_receive_frame.data_len = data[i];
                    s_receive_check_sum += data[i];
                    s_parse_state = RECEIVE_LEN_H_STATE;
                }
                break;
                
                case RECEIVE_LEN_H_STATE:
                {
                    s_receive_frame.data_len |= (data[i] << 8);
                    s_receive_check_sum += data[i];
                    if(s_receive_frame.data_len == 0)
                    {
                        s_parse_state = RECEIVE_CHECK_SUM_L_STATE;
                    }
                    else if(s_receive_frame.data_len >= RECEIVE_MAX_LEN)
                    {
                        s_parse_state = CHECK_FRAME_L_STATE;
                    }
                    else
                    {
                        s_receive_data_count = 0;
                        s_parse_state = RECEIVE_DATA_STATE;
                    }
                }
                break;
                
                case RECEIVE_DATA_STATE:
                {
                    s_receive_frame.data[s_receive_data_count] = data[i];    
                    if(++s_receive_data_count == s_receive_frame.data_len)
                    {
                        s_parse_state = RECEIVE_CHECK_SUM_L_STATE;
                    }
                }
                break;
                
                case RECEIVE_CHECK_SUM_L_STATE:
                {
                    s_receive_frame.check_sum = data[i];
                    s_parse_state = RECEIVE_CHECK_SUM_H_STATE;
                }
                break;
                
                case RECEIVE_CHECK_SUM_H_STATE:
                {
                    s_receive_frame.check_sum |= (data[i] << 8);
                    s_parse_state = CHECK_FRAME_L_STATE;
                    cm_cmd_check();
                }
                break;
                
                default:{s_parse_state=CHECK_FRAME_L_STATE;}break;
            }
        }
    }
}

void cm_init(cm_func_cfg_t *dfu_m_func_cfg, uint16_t once_send_size)
{
    if(once_send_size != 0)
    {
      s_once_size = once_send_size; 
    }

    if(dfu_m_func_cfg != NULL)
    {
        s_p_func_cfg = dfu_m_func_cfg;
    }
}

void cm_parse_state_reset(void)
{
    s_parse_state = CHECK_FRAME_L_STATE;
    s_cmd_receive_flag   = false;
    s_receive_data_count = 0;
    s_receive_check_sum  = 0;
}

void cm_schedule(cm_rev_cmd_cb_t rev_cmd_cb)
{
    if(s_cmd_receive_flag)
    {
        if (rev_cmd_cb)
        {
            rev_cmd_cb();
        }
        
        cm_event_handler(true, &s_receive_frame);

        s_cmd_receive_flag = 0;
    }
}

