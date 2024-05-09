#include "multi_slave_manage.h"
#include "multi_slave_config.h"
#include "app_error.h"
#include "app_log.h"
#include "long_term_user.h"
/*
 * EXTERNAL SYMBOLS DEFINITIONS
 *****************************************************************************************
 */

static const multi_dev_func_t *s_slaves_func[MAX_SLAVE] = ALL_SLAVE_FUNC_INCO;
static uint8_t multi_slave_addr[MAX_SLAVE][6] = {0,0};
static uint8_t  s_multi_slave_conn_idx[MAX_SLAVE];
static slave_state_t s_multi_slave_state[MAX_SLAVE];

static bool s_need_start_adv[MAX_SLAVE] = {0};
static uint8_t s_need_start_adv_count = 0;
static bool s_start_use_flag = false;

static bool s_need_stop_adv[MAX_SLAVE] ={0};
static uint8_t s_need_stop_adv_count = 0;
bool s_stop_use_flag = false;


static void multi_slave_addr_init(void)
{
    sdk_err_t         error_code;
    ble_gap_bdaddr_t  bd_addr;
    error_code = ble_gap_addr_get(&bd_addr);
    APP_ERROR_CHECK(error_code);
    APP_LOG_INFO("Local Board %02X:%02X:%02X:%02X:%02X:%02X.",
                 bd_addr.gap_addr.addr[5],
                 bd_addr.gap_addr.addr[4],
                 bd_addr.gap_addr.addr[3],
                 bd_addr.gap_addr.addr[2],
                 bd_addr.gap_addr.addr[1],
                 bd_addr.gap_addr.addr[0]);
    
    for(uint8_t i=0; i<MAX_SLAVE; i++)
    {
        bd_addr.gap_addr.addr[0] += i;
        memcpy(&multi_slave_addr[i][0], bd_addr.gap_addr.addr, 6);
    }
}

static void multi_salve_start_adv_check(uint8_t adv_idx)
{
    if(s_need_start_adv_count != 0)
    {
        while(s_start_use_flag);
        s_start_use_flag = true;
        s_need_start_adv_count -=1;
        s_need_start_adv[adv_idx] = false;
        if(s_need_start_adv_count != 0)
        {
            for(uint8_t i=0; i<MAX_SLAVE; i++)
            {
                if(s_need_start_adv[i])
                {
                    s_multi_slave_state[i] = ADV_START_STATE;
                    s_slaves_func[i]->adv_start();
                    break;
                }
            }
        }
        s_start_use_flag = false;
    }
}

static void multi_salve_stop_adv_check(uint8_t adv_idx)
{
    if(s_need_stop_adv_count != 0)
    {
        while(s_stop_use_flag);
        s_stop_use_flag = true;
        s_need_stop_adv_count -=1;
        s_need_stop_adv[adv_idx] = false;
        if(s_need_stop_adv_count != 0)
        {
            for(uint8_t i=0; i<MAX_SLAVE; i++)
            {
                if(s_need_stop_adv[i])
                {
                    s_multi_slave_state[i] = ADV_STOP_STATE;
                    ble_gap_adv_stop(i);
                    break;
                }
            }
        }
        s_stop_use_flag = false;
    }
}

static bool multi_slave_cmp_addr(const uint8_t *addr, uint8_t *p_out_index)
{
    for(uint8_t i=0; i<MAX_SLAVE; i++)
    {
        if(memcmp(addr, multi_slave_addr[i], 6) == 0)
        {
            *p_out_index = i;
            return true;
        }
    }
    return false;
}

static void multi_slave_hide_services(uint8_t adv_idx, uint8_t conn_idx)
{
    ble_gatts_service_hide_clear(conn_idx);
    for(uint8_t i=0; i<MAX_SLAVE; i++)
    {
        if(i != adv_idx)
        {
            s_slaves_func[i]->hide_services(conn_idx);
        }
    }
}

void mult_slave_ble_evt_handler(const ble_evt_t *p_evt)
{
    uint8_t local_con_addr[6];
    uint8_t index = p_evt->evt.gapc_evt.index;
    uint8_t adv_idx = 0;
    switch(p_evt->evt_id)
    {
        case BLE_COMMON_EVT_STACK_INIT:
            multi_slave_addr_init();
            for(uint8_t i=0; i<MAX_SLAVE; i++)
            {
                s_slaves_func[i]->adv_params_init();
                s_multi_slave_conn_idx[i] = BLE_GAP_INVALID_CONN_INDEX;
                s_multi_slave_state[i] = INIT_STATE;
            }
            //根据绑定情况，确认开启什么广播
            multi_slave_start_adv(COMMON_SLAVE_ADV_INDEX);
            if(get_bonded_flag())
            {
                multi_slave_start_adv(HID_SLAVE_ADV_INDEX);
            }
        break;
        
         case BLE_GAPM_EVT_ADV_START:
             
             s_multi_slave_state[index] = ADV_STATE;
             s_slaves_func[index]->ble_evt_handler(p_evt);
             multi_salve_start_adv_check(index);
             break;
         
         case BLE_GAPM_EVT_ADV_STOP:
             s_multi_slave_state[index] = INIT_STATE;
             s_slaves_func[index]->ble_evt_handler(p_evt);
             multi_salve_stop_adv_check(index);
             break;
         
         case BLE_GAPC_EVT_CONNECTED:
             ble_gap_conn_local_addr_get(index,local_con_addr);
             if(multi_slave_cmp_addr(local_con_addr, &adv_idx))
             {
                 s_multi_slave_state[index] = CONN_STATE;
                 s_multi_slave_conn_idx[adv_idx] = index;  // 连接上
                 multi_slave_hide_services(adv_idx, index);//将其他slave Service进行隐藏
                 s_slaves_func[adv_idx]->ble_evt_handler(p_evt);
             }
             
             break;
             
          case BLE_GAPC_EVT_CONN_PARAM_UPDATE_REQ:
            ble_gap_conn_param_update_reply(index, true);
            break;
             
         default:
             for(uint8_t i=0; i<MAX_SLAVE; i++)
             {
                 if(index == s_multi_slave_conn_idx[i])
                 {
                     if(p_evt->evt_id == BLE_GAPC_EVT_DISCONNECTED)
                     {
                         s_multi_slave_state[i] = INIT_STATE;
                         s_multi_slave_conn_idx[i] = BLE_GAP_INVALID_CONN_INDEX;
                     }
                     s_slaves_func[i]->ble_evt_handler(p_evt);
                     break;
                 }
             }
             break;
    }
}

void multi_slave_services_init(void)
{
    for(uint8_t i=0; i<MAX_SLAVE; i++)
    {
        s_slaves_func[i]->services_init();
    }
}

uint8_t* multi_slave_get_addr(uint8_t adv_idx)
{
    return &multi_slave_addr[adv_idx][0];
}

uint8_t  multi_slave_get_conn_idx(uint8_t adv_idx)
{
    return s_multi_slave_conn_idx[adv_idx];
}

uint8_t multi_slave_get_state(uint8_t adv_idx)
{
    return s_multi_slave_state[adv_idx];
}

/*-----------------------------------------------------------*/
bool multi_slave_disconnect(uint8_t adv_idx)
{
    if(s_multi_slave_state[adv_idx] == CONN_STATE)
    {
        ble_gap_disconnect(multi_slave_get_conn_idx(adv_idx));
        return true;
    }
    return false;
}

bool multi_slave_stop_adv(uint8_t adv_idx)
{
    uint32_t timeout = 0;
    if(s_multi_slave_state[adv_idx] == ADV_START_STATE)
    {
        while(s_multi_slave_state[adv_idx] == ADV_START_STATE)
        {
            timeout++;
            if(timeout > 1000)
            {
                return false;
            }
        }
    }
    
    if(s_multi_slave_state[adv_idx] == ADV_STATE)
    {
        while(s_stop_use_flag);
        s_stop_use_flag = true;
        s_need_stop_adv[adv_idx] = true;
        s_need_stop_adv_count += 1;
        if(s_need_stop_adv_count == 1)
        {
            s_multi_slave_state[adv_idx] = ADV_STOP_STATE;
            ble_gap_adv_stop(adv_idx);
        }
        s_stop_use_flag = false;
    }
    else if(s_multi_slave_state[adv_idx]  == CONN_STATE)
    {
        return false;
    }
    return true;
}

bool multi_slave_start_adv(uint8_t adv_idx)
{
    uint32_t timeout = 0;
    if(s_multi_slave_state[adv_idx] == ADV_STOP_STATE)
    {
        while(s_multi_slave_state[adv_idx] == ADV_STOP_STATE)
        {
            timeout++;
            if(timeout > 1000)
            {
                return false;
            }
        }
    }
    if(s_multi_slave_state[adv_idx] == INIT_STATE)
    {
        while(s_start_use_flag);
        s_start_use_flag = true;
        s_need_start_adv[adv_idx] = true;
        s_need_start_adv_count += 1;
        if(s_need_start_adv_count == 1)
        {
            s_multi_slave_state[adv_idx] = ADV_START_STATE;
            s_slaves_func[adv_idx]->adv_start();//start common adv
        }
        s_start_use_flag = false;
    }
    else if(s_multi_slave_state[adv_idx] == CONN_STATE)
    {
        return false;
    }
    return true;
}



