/**
 *****************************************************************************************
 *
 * @file user_app.c
 *
 * @brief User function Implementation.
 *
 *****************************************************************************************
 * @attention
  #####Copyright (c) 2019 GOODIX
  All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:
  * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
  * Neither the name of GOODIX nor the names of its contributors may be used
    to endorse or promote products derived from this software without
    specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED. IN NO EVENT SHALL COPYRIGHT HOLDERS AND CONTRIBUTORS BE
  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************************
 */

/*
 * INCLUDE FILES
 *****************************************************************************************
 */
#include "user_app.h"
#include "grx_sys.h"
#include "app_log.h"
#include "app_error.h"
#include "multi_slave_manage.h"
#include "multi_slave_config.h"

#define DEVICE_NAME                        "Goodix_EBike"       /**< Device Name which will be set in GAP. */
#define MIN_CONN_INTERVAL                  400                  /**< Minimum acceptable connection interval (0.4 seconds). */
#define MAX_CONN_INTERVAL                  650                  /**< Maximum acceptable connection interval (0.65 second). */
#define SLAVE_LATENCY                      0                    /**< Slave latency. */
#define CONN_SUP_TIMEOUT                   4000                 /**< Connection supervisory timeout (4 seconds). */

static void gap_params_init(void)
{
    sdk_err_t        error_code;
    ble_gap_conn_param_t gap_conn_param;

    ble_gap_pair_enable(true);

    error_code = ble_gap_device_name_set(BLE_GAP_WRITE_PERM_DISABLE,
                                         (uint8_t *)DEVICE_NAME, strlen(DEVICE_NAME));
    APP_ERROR_CHECK(error_code);

    gap_conn_param.interval_min  = MIN_CONN_INTERVAL;
    gap_conn_param.interval_max  = MAX_CONN_INTERVAL;
    gap_conn_param.slave_latency = SLAVE_LATENCY;
    gap_conn_param.sup_timeout   = CONN_SUP_TIMEOUT;
    error_code = ble_gap_ppcp_set(&gap_conn_param);
    APP_ERROR_CHECK(error_code);

    ble_sec_param_t sec_param =
    {
        .level     = BLE_SEC_MODE1_LEVEL3,
        .io_cap    = BLE_SEC_IO_DISPLAY_ONLY,
        .oob       = false,
        .auth      = BLE_SEC_AUTH_BOND | BLE_SEC_AUTH_MITM | BLE_SEC_AUTH_SEC_CON,
        .key_size  = 16,
        .ikey_dist = BLE_SEC_KDIST_ALL,
        .rkey_dist = BLE_SEC_KDIST_ALL,
    };
    error_code = ble_sec_params_set(&sec_param);
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_privacy_params_set(150, true);
    APP_ERROR_CHECK(error_code);
}

static void ble_app_init(void)
{
    sdk_version_t     version;

    sys_sdk_verison_get(&version);
    APP_LOG_INFO("Goodix BLE SDK V%d.%d.%d (commit %x)",
                 version.major, version.minor, version.build, version.commit_id);
    gap_params_init();
    multi_slave_services_init();
}

/*
 * GLOBAL FUNCTION DEFINITIONS
 *******************************************************************************
 */
void ble_evt_handler(const ble_evt_t *p_evt)
{
    if(p_evt == NULL)
    {
        return;
    }
    if(p_evt->evt_id == BLE_COMMON_EVT_STACK_INIT)
    {
        ble_app_init();
    }
    mult_slave_ble_evt_handler(p_evt);
}




