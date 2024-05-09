#ifndef _MULTI_SLAVE_MANAGE_H_
#define _MULTI_SLAVE_MANAGE_H_
#include "grx_sys.h"

typedef enum
{
    INIT_STATE,
    ADV_START_STATE,
    ADV_STATE,
    ADV_STOP_STATE,
    CONN_STATE,
}slave_state_t;

void multi_slave_services_init(void);
void mult_slave_ble_evt_handler(const ble_evt_t *p_evt);
uint8_t  multi_slave_get_conn_idx(uint8_t adv_idx);
uint8_t* multi_slave_get_addr(uint8_t adv_idx);
bool multi_slave_disconnect(uint8_t adv_idx);
bool multi_slave_stop_adv(uint8_t adv_idx);
bool multi_slave_start_adv(uint8_t adv_idx);
uint8_t multi_slave_get_state(uint8_t adv_idx);
#endif

