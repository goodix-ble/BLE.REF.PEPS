
#include "fmna_peer_manager_platform.h"

#include "fmna_log_platform.h"

#include "custom_config.h"


uint32_t fmna_pm_peer_count(void)
{
    fmna_ret_code_t         err_code;
    ble_gap_bond_dev_list_t bond_list;

#if defined(SOC_GR5515)
    err_code = ble_gap_bond_devs_get(&bond_list);
#elif defined(SOC_GR5332) 
    ble_gap_bdaddr_t        bdaddr[10];

    bond_list.items = bdaddr;
    err_code = ble_gap_bond_devs_get(&bond_list, 10);
#endif
    if (err_code != FMNA_SUCCESS)
    {
        FMNA_ERROR_CHECK(err_code);
        bond_list.num = 0;
    }

    return bond_list.num;
}

void fmna_pm_delete_bonds(void)
{
    fmna_ret_code_t err_code;

    err_code = ble_gap_bond_devs_clear();
    FMNA_ERROR_CHECK(err_code);

    err_code = ble_gap_whitelist_clear();
    FMNA_ERROR_CHECK(err_code);
}

void fmna_peer_manager_init(void)
{
    fmna_ret_code_t     err_code;
    ble_sec_param_t     sec_param;

    ble_gap_pair_enable(true);

    err_code = ble_gap_privacy_params_set(FMNA_BLE_PRIVACY_RENEW_DURATION, true);
    FMNA_ERROR_CHECK(err_code);

    sec_param.level     = FMNA_BLE_SEC_LEVEL;
    sec_param.io_cap    = FMNA_BLE_SEC_IO_CAP;
    sec_param.oob       = FMNA_BLE_SEC_OOB;
    sec_param.auth      = FMNA_BLE_SEC_AUTH;
    sec_param.key_size  = FMNA_BLE_SEC_KEY_SIZE;
    sec_param.ikey_dist = FMNA_BLE_SEC_IKEY_DIST;
    sec_param.rkey_dist = FMNA_BLE_SEC_RKEY_DIST;

    err_code = ble_sec_params_set(&sec_param);
    FMNA_ERROR_CHECK(err_code);
}

