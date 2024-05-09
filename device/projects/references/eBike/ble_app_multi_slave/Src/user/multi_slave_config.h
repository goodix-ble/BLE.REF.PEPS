#ifndef _MULTI_SLAVE_CONFIG_H_
#define _MULTI_SLAVE_CONFIG_H_

#include "grx_sys.h"
#include <stdint.h>

#define MAX_SLAVE_DEVICES                  5

extern void ble_gap_conn_local_addr_get(uint8_t conn_idx, uint8_t *p_addr);                    /**< Get the location mac address of the current connection. */
extern uint16_t ble_gatts_service_hide_set(uint8_t conn_idx, uint16_t handle);                 /**< Set whether the corresponding service is discovered according to the service handle. */
extern uint16_t ble_gatts_service_hide_clear(uint8_t conn_idx);                                /**< Clean the service hided setting. */


typedef struct
{
    void(*adv_params_init)(void);
    void(*ble_evt_handler)(const ble_evt_t *p_evt);
    void(*adv_start)(void);
    void(*hide_services)(uint8_t conn_idx);
    void(*services_init)(void);
}multi_dev_func_t;


/*-----------------------------------------------------------------------------------------------*/
enum
{
    COMMON_SLAVE_ADV_INDEX,
    HID_SLAVE_ADV_INDEX,
    FMNA_SLAVE_ADV_INDEX,
    MAX_SLAVE,
};

extern const multi_dev_func_t com_slave_func;
extern const multi_dev_func_t hid_slave_func;
extern const multi_dev_func_t fmna_slave_func;

#define ALL_SLAVE_FUNC_INCO  {&com_slave_func, &hid_slave_func, &fmna_slave_func, }
    
#endif


