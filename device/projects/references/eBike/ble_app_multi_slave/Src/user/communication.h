#ifndef _COMMUNICATION_H_
#define _COMMUNICATION_H_
#include <stdint.h>
#include <stdbool.h>

#define RECEIVE_MAX_LEN         1024

typedef struct
{
    uint16_t cmd_type;
    uint16_t data_len;
    uint8_t  data[RECEIVE_MAX_LEN];
    uint16_t check_sum;
} receive_frame_t;


typedef struct
{
    void (*cm_send_data)(uint8_t *data, uint16_t len);
    void (*cm_event_handler)(bool status, receive_frame_t *p_frame);
}cm_func_cfg_t;


typedef void (*cm_rev_cmd_cb_t)(void);


void cm_init(cm_func_cfg_t *dfu_m_func_cfg, uint16_t once_send_size);
void cm_parse_state_reset(void);
void cm_schedule(cm_rev_cmd_cb_t rev_cmd_cb);
void cm_cmd_prase(uint8_t* data, uint16_t len);
void cm_send_data_cmpl_process(void);
void cm_send_frame(uint8_t *data,uint16_t len,uint16_t cmd_type);

#endif


