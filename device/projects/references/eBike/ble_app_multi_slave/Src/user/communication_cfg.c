#include "communication_cfg.h"
#include "communication.h"
#include "multi_slave_config.h"
#include "multi_slave_manage.h"
#include "gus.h"

static void gus_data_send(uint8_t *p_data, uint16_t length);
static void cm_event_handler(bool status, receive_frame_t *p_frame);

static cm_func_cfg_t s_dfu_m_func_cfg = 
{
    .cm_send_data     = gus_data_send,
    .cm_event_handler = cm_event_handler,
};


static void gus_data_send(uint8_t *p_data, uint16_t length)
{
    gus_tx_data_send(multi_slave_get_conn_idx(COMMON_SLAVE_ADV_INDEX), p_data, length);
}

static void cm_event_handler(bool status, receive_frame_t *p_frame)
{
    //uint8_t reset_flag = 0;
    //uint8_t send_data[28];
    switch(p_frame->cmd_type)
    {
        case 0x0001://进行连接认证
            
            break;
        
        case 0x0002:
            break;
        
        case 0x0003:
            break;
        
        
        default:
            break;
    }
}


void com_cfg_init(void)
{
    cm_init(&s_dfu_m_func_cfg, 244);
}


void com_send_device_state(uint8_t device_type, bool connect)
{
    uint8_t send_data[2] = {0x00,0x00};
    send_data[0] = device_type;
    send_data[1] = connect;
    cm_send_frame(send_data,2,0x0100);
}
