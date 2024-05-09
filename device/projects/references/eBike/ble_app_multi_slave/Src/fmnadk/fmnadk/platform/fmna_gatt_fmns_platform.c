

#include "fmna_gatt_fmns_platform.h"
#include "fmna_gatt_tps_platform.h"
#include "fmna_log_platform.h"
#include "fmna_constants.h"
#include "fmna_gatt.h"
#include "fmna_version.h"
#include "fmna_util.h"


#if FMNA_UARP_ENABLE
#include "fmna_uarp_control_point.h"
#endif

/**@brief Macros for conversion of 128bit to 16bit UUID. */
#define ATT_128_PRIMARY_SERVICE BLE_ATT_16_TO_128_ARRAY(BLE_ATT_DECL_PRIMARY_SERVICE)
#define ATT_128_CHARACTERISTIC  BLE_ATT_16_TO_128_ARRAY(BLE_ATT_DECL_CHARACTERISTIC)
#define ATT_128_CLIENT_CHAR_CFG BLE_ATT_16_TO_128_ARRAY(BLE_ATT_DESC_CLIENT_CHAR_CFG)


#define AIS_SERVICE_UUID                    {0x8B, 0x47, 0x38, 0xDC, 0xB9, 0x11, 0xA9, 0xA1, 0xB1, 0x43, 0x51, 0x3C, 0x02, 0x01, 0x29, 0x87}
#define AIS_PRODUCT_DATA_UUID               {0x0B, 0xBB, 0x6F, 0x41, 0x3A, 0x00, 0xB4, 0xA7, 0x57, 0x4D, 0x52, 0x63, 0x01, 0x00, 0xA5, 0X6A}
#define AIS_MANUFA_NAME_UUID                {0x0B, 0xBB, 0x6F, 0x41, 0x3A, 0x00, 0xB4, 0xA7, 0x57, 0x4D, 0x52, 0x63, 0x02, 0x00, 0xA5, 0X6A}
#define AIS_MODEL_NAME_UUID                 {0x0B, 0xBB, 0x6F, 0x41, 0x3A, 0x00, 0xB4, 0xA7, 0x57, 0x4D, 0x52, 0x63, 0x03, 0x00, 0xA5, 0X6A}
#define AIS_ACCESS_CATE_UUID                {0x0B, 0xBB, 0x6F, 0x41, 0x3A, 0x00, 0xB4, 0xA7, 0x57, 0x4D, 0x52, 0x63, 0x05, 0x00, 0xA5, 0X6A}
#define AIS_ACCESS_CAPA_UUID                {0x0B, 0xBB, 0x6F, 0x41, 0x3A, 0x00, 0xB4, 0xA7, 0x57, 0x4D, 0x52, 0x63, 0x06, 0x00, 0xA5, 0X6A}
#define AIS_FW_VERSION_UUID                 {0x0B, 0xBB, 0x6F, 0x41, 0x3A, 0x00, 0xB4, 0xA7, 0x57, 0x4D, 0x52, 0x63, 0x07, 0x00, 0xA5, 0X6A}
#define AIS_FMNA_VERSION_UUID               {0x0B, 0xBB, 0x6F, 0x41, 0x3A, 0x00, 0xB4, 0xA7, 0x57, 0x4D, 0x52, 0x63, 0x08, 0x00, 0xA5, 0X6A}
#define AIS_BATTERY_TYPE_UUID               {0x0B, 0xBB, 0x6F, 0x41, 0x3A, 0x00, 0xB4, 0xA7, 0x57, 0x4D, 0x52, 0x63, 0x09, 0x00, 0xA5, 0X6A}
#define AIS_BATTERY_LEVEL_UUID              {0x0B, 0xBB, 0x6F, 0x41, 0x3A, 0x00, 0xB4, 0xA7, 0x57, 0x4D, 0x52, 0x63, 0x0A, 0x00, 0xA5, 0X6A}

#define FMNA_SERVICE_UUID                   {0xFB, 0x34, 0x9B, 0x5F, 0x80, 0x00, 0x00, 0x80, 0x00, 0x10, 0x00, 0x00, 0x44, 0xFD, 0x00, 0x00}
#define FMNA_PAIRING_CP_UUID                {0x7A, 0x42, 0x04, 0x03, 0x73, 0x2F, 0xD4, 0xBE, 0xEF, 0x49, 0x3B, 0x94, 0x01, 0x00, 0x86, 0x4F}
#define FMNA_CONFIG_CP_UUID                 {0x7A, 0x42, 0x04, 0x03, 0x73, 0x2F, 0xD4, 0xBE, 0xEF, 0x49, 0x3B, 0x94, 0x02, 0x00, 0x86, 0x4F}
#define FMNA_NON_OWNER_CP_UUID              {0x7A, 0x42, 0x04, 0x03, 0x73, 0x2F, 0xD4, 0xBE, 0xEF, 0x49, 0x3B, 0x94, 0x03, 0x00, 0x86, 0x4F}
#define FMNA_PAIRED_OWNER_INFO_CP_UUID      {0x7A, 0x42, 0x04, 0x03, 0x73, 0x2F, 0xD4, 0xBE, 0xEF, 0x49, 0x3B, 0x94, 0x04, 0x00, 0x86, 0x4F}
#define FMNA_DEBUG_INFO_CP_UUID             {0x7A, 0x42, 0x04, 0x03, 0x73, 0x2F, 0xD4, 0xBE, 0xEF, 0x49, 0x3B, 0x94, 0x05, 0x00, 0x86, 0x4F}

#define UARP_SERVICE_UUID                   {0xFB, 0x34, 0x9B, 0x5F, 0x80, 0x00, 0x00, 0x80, 0x00, 0x10, 0x00, 0x00, 0x43, 0xFD, 0x00, 0x00}
#define UARP_DATA_CHAR_UUID                 {0xDE, 0xB0, 0x01, 0x7F, 0x4A, 0x6A, 0xF1, 0xA4, 0x25, 0x42, 0x9B, 0x6D, 0x01, 0x00, 0x11, 0x94}

enum
{
    AIS_IDX_SVC,
    AIS_IDX_PRODUCT_DATA_CHAR,
    AIS_IDX_PRODUCT_DATA_VAL,
    AIS_IDX_MANUFA_NAME_CHAR,
    AIS_IDX_MANUFA_NAME_VAL,
    AIS_IDX_MODEL_NAME_CHAR,
    AIS_IDX_MODEL_NAME_VAL,
    AIS_IDX_ACCESS_CATE_CHAR,
    AIS_IDX_ACCESS_CATE_VAL,
    AIS_IDX_ACCESS_CAPA_CHAR,
    AIS_IDX_ACCESS_CAPA_VAL,
    AIS_IDX_FW_VERSION_CHAR,
    AIS_IDX_FW_VERSION_VAL,
    AIS_IDX_FMNA_VERSION_CHAR,
    AIS_IDX_FMNA_VERSION_VAL,
    AIS_IDX_BATTERY_LEVEL_CHAR,
    AIS_IDX_BATTERY_LEVEL_VAL,
    AIS_IDX_BATTERY_TYPE_CHAR,
    AIS_IDX_BATTERY_TYPE_VAL,
    AIS_IDX_NB,
};

enum
{
    FMNA_IDX_SVC,
    FMNA_IDX_PAIRING_CP_CHAR,
    FMNA_IDX_PAIRING_CP_VAL,
    FMNA_IDX_PAIRING_CP_CFG,
    FMNA_IDX_CONFIG_CP_CHAR,
    FMNA_IDX_CONFIG_CP_VAL,
    FMNA_IDX_CONFIG_CP_CFG,
    FMNA_IDX_NON_OWNER_CHAR,
    FMNA_IDX_NON_OWNER_VAL,
    FMNA_IDX_NON_OWNER_CFG,
    FMNA_IDX_PAIRED_OWNER_INFO_CP_CHAR,
    FMNA_IDX_PAIRED_OWNER_INFO_CP_VAL,
    FMNA_IDX_PAIRED_OWNER_INFO_CP_CFG,
#if FMNA_DEBUG_ENABLE
    FMNA_IDX_DEBUG_CP_CHAR,
    FMNA_IDX_DEBUG_CP_VAL,
    FMNA_IDX_DEBUG_CP_CFG,
#endif
    FMNA_IDX_NB,
};

enum
{
    UARP_IDX_SVC,
    UARP_DATA_TRAN_CHAR,
    UARP_DATA_TRAN_VAL,
    UARP_DATA_TRAN_CFG,
    UARP_IDX_NB,
};

static const ble_gatts_attm_desc_128_t ais_att_db[AIS_IDX_NB] =
{
    [AIS_IDX_SVC]                     = {ATT_128_PRIMARY_SERVICE, BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [AIS_IDX_PRODUCT_DATA_CHAR]       = {ATT_128_CHARACTERISTIC,  BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [AIS_IDX_PRODUCT_DATA_VAL]        = {AIS_PRODUCT_DATA_UUID,   BLE_GATTS_READ_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128),FMNA_PRODUCT_DATA_LEN},
    [AIS_IDX_MANUFA_NAME_CHAR]        = {ATT_128_CHARACTERISTIC,  BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [AIS_IDX_MANUFA_NAME_VAL]         = {AIS_MANUFA_NAME_UUID,    BLE_GATTS_READ_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128),FMNA_MANUFACTURER_NAME_LEN},
    [AIS_IDX_MODEL_NAME_CHAR]         = {ATT_128_CHARACTERISTIC,  BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [AIS_IDX_MODEL_NAME_VAL]          = {AIS_MODEL_NAME_UUID,     BLE_GATTS_READ_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128),FMNA_MODEL_NAME_LEN},
    [AIS_IDX_ACCESS_CATE_CHAR]        = {ATT_128_CHARACTERISTIC,  BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [AIS_IDX_ACCESS_CATE_VAL]         = {AIS_ACCESS_CATE_UUID,    BLE_GATTS_READ_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128),FMNA_ACC_CATEGORY_LEN},
    [AIS_IDX_ACCESS_CAPA_CHAR]        = {ATT_128_CHARACTERISTIC,  BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [AIS_IDX_ACCESS_CAPA_VAL]         = {AIS_ACCESS_CAPA_UUID,    BLE_GATTS_READ_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128),ACC_CAP_MAX_LEN},
    [AIS_IDX_FW_VERSION_CHAR]         = {ATT_128_CHARACTERISTIC,  BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [AIS_IDX_FW_VERSION_VAL]          = {AIS_FW_VERSION_UUID,     BLE_GATTS_READ_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128),FW_VERS_MAX_LEN},
    [AIS_IDX_FMNA_VERSION_CHAR]       = {ATT_128_CHARACTERISTIC,  BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [AIS_IDX_FMNA_VERSION_VAL]        = {AIS_FMNA_VERSION_UUID,   BLE_GATTS_READ_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128),FINDMY_VERS_MAX_LEN},
    [AIS_IDX_BATTERY_LEVEL_CHAR]      = {ATT_128_CHARACTERISTIC,  BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [AIS_IDX_BATTERY_LEVEL_VAL]       = {AIS_BATTERY_LEVEL_UUID,  BLE_GATTS_READ_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128),BATT_LVL_MAX_LEN},
    [AIS_IDX_BATTERY_TYPE_CHAR]       = {ATT_128_CHARACTERISTIC,  BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [AIS_IDX_BATTERY_TYPE_VAL]        = {AIS_BATTERY_TYPE_UUID,   BLE_GATTS_READ_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128),BATT_TYPE_MAX_LEN},
};

static const ble_gatts_attm_desc_128_t fmna_att_db[FMNA_IDX_NB] =
{
    [FMNA_IDX_SVC]                       = {ATT_128_PRIMARY_SERVICE,        BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [FMNA_IDX_PAIRING_CP_CHAR]           = {ATT_128_CHARACTERISTIC,         BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [FMNA_IDX_PAIRING_CP_VAL]            = {FMNA_PAIRING_CP_UUID,           BLE_GATTS_WRITE_REQ_PERM_UNSEC | BLE_GATTS_INDICATE_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128), FMNA_BLE_GATT_PAYLOAD_LEN(FMNA_BLE_MTU_SIZE)},
    [FMNA_IDX_PAIRING_CP_CFG]            = {ATT_128_CLIENT_CHAR_CFG,        BLE_GATTS_READ_PERM_UNSEC      | BLE_GATTS_WRITE_REQ_PERM(BLE_GAP_WRITE_PERM_NOAUTH), 0, 0},

    [FMNA_IDX_CONFIG_CP_CHAR]            = {ATT_128_CHARACTERISTIC,         BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [FMNA_IDX_CONFIG_CP_VAL]             = {FMNA_CONFIG_CP_UUID,            BLE_GATTS_WRITE_REQ_PERM_UNSEC | BLE_GATTS_INDICATE_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128), FMNA_BLE_GATT_PAYLOAD_LEN(FMNA_BLE_MTU_SIZE)},
    [FMNA_IDX_CONFIG_CP_CFG]             = {ATT_128_CLIENT_CHAR_CFG,        BLE_GATTS_READ_PERM_UNSEC      | BLE_GATTS_WRITE_REQ_PERM(BLE_GAP_WRITE_PERM_NOAUTH), 0, 0},

    [FMNA_IDX_NON_OWNER_CHAR]            = {ATT_128_CHARACTERISTIC,         BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [FMNA_IDX_NON_OWNER_VAL]             = {FMNA_NON_OWNER_CP_UUID,         BLE_GATTS_WRITE_REQ_PERM_UNSEC | BLE_GATTS_INDICATE_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128), NONOWN_MAX_LEN},
    [FMNA_IDX_NON_OWNER_CFG]             = {ATT_128_CLIENT_CHAR_CFG,        BLE_GATTS_READ_PERM_UNSEC      | BLE_GATTS_WRITE_REQ_PERM_UNSEC, 0, 0},

    [FMNA_IDX_PAIRED_OWNER_INFO_CP_CHAR] = {ATT_128_CHARACTERISTIC,         BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [FMNA_IDX_PAIRED_OWNER_INFO_CP_VAL]  = {FMNA_PAIRED_OWNER_INFO_CP_UUID, BLE_GATTS_WRITE_REQ_PERM_UNSEC | BLE_GATTS_INDICATE_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128), PAIRED_OWNER_MAX_LEN},
    [FMNA_IDX_PAIRED_OWNER_INFO_CP_CFG]  = {ATT_128_CLIENT_CHAR_CFG,        BLE_GATTS_READ_PERM_UNSEC      | BLE_GATTS_WRITE_REQ_PERM_UNSEC, 0, 0},

#if FMNA_DEBUG_ENABLE
    [FMNA_IDX_DEBUG_CP_CHAR]             = {ATT_128_CHARACTERISTIC,         BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [FMNA_IDX_DEBUG_CP_VAL]              = {FMNA_DEBUG_INFO_CP_UUID,        BLE_GATTS_WRITE_REQ_PERM_UNSEC | BLE_GATTS_INDICATE_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128), FMNA_BLE_GATT_PAYLOAD_LEN(FMNA_BLE_MTU_SIZE)},
    [FMNA_IDX_DEBUG_CP_CFG]              = {ATT_128_CLIENT_CHAR_CFG,        BLE_GATTS_READ_PERM_UNSEC      | BLE_GATTS_WRITE_REQ_PERM_UNSEC, 0, 0},
#endif

};
#if FMNA_UARP_ENABLE
static const ble_gatts_attm_desc_128_t uarp_att_db[UARP_IDX_NB] =
{
    [UARP_IDX_SVC]                       = {ATT_128_PRIMARY_SERVICE,        BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [UARP_DATA_TRAN_CHAR]                = {ATT_128_CHARACTERISTIC,         BLE_GATTS_READ_PERM_UNSEC, 0, 0},
    [UARP_DATA_TRAN_VAL]                 = {UARP_DATA_CHAR_UUID,            BLE_GATTS_WRITE_REQ_PERM_UNSEC | BLE_GATTS_INDICATE_PERM_UNSEC, BLE_GATTS_ATT_VAL_LOC_USER | BLE_GATTS_ATT_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128), FMNA_BLE_GATT_PAYLOAD_LEN(FMNA_BLE_MTU_SIZE)},
    [UARP_DATA_TRAN_CFG]                 = {ATT_128_CLIENT_CHAR_CFG,        BLE_GATTS_READ_PERM_UNSEC      | BLE_GATTS_WRITE_REQ_PERM(BLE_GAP_WRITE_PERM_NOAUTH), 0, 0},
};
#endif

#if FMNA_UARP_ENABLE
static void       uarp_write_att_evt_handler(uint8_t conn_idx, const ble_gatts_evt_write_t *p_param);
static void       uarp_ntf_ind_evt_handler(uint8_t conn_idx, uint8_t status, const ble_gatts_evt_ntf_ind_t *p_ntf_ind);
static void       uarp_ble_evt_handler(const ble_evt_t *p_evt);
#endif

static void       ais_ble_evt_handler(const ble_evt_t *p_evt);
static void       fmna_ble_evt_handler(const ble_evt_t *p_evt);

static void       ais_read_att_evt_handler(uint8_t conn_idx, const ble_gatts_evt_read_t *p_param);
static void       fmna_write_att_evt_handler(uint8_t conn_idx, const ble_gatts_evt_write_t *p_param);
static void       fmna_ntf_ind_evt_handler(uint8_t conn_idx, uint8_t status, const ble_gatts_evt_ntf_ind_t *p_ntf_ind);

/*
 * LOCAL VARIABLE DEFINITIONS
 *****************************************************************************************
 */
typedef struct
{
    ble_gatts_create_db_t   ais_gatts_db;
    ble_gatts_create_db_t   fmna_gatts_db;
#if FMNA_UARP_ENABLE
    ble_gatts_create_db_t   uarp_gatts_db;
#endif
    uint16_t                ais_start_hdl;
    uint16_t                fmna_start_hdl;
#if FMNA_UARP_ENABLE
    uint16_t                uarp_start_hdl;
#endif
    uint32_t                ais_char_mask;
    uint32_t                fmna_char_mask;
#if FMNA_UARP_ENABLE
    uint32_t                uarp_char_mask;
#endif
} fmna_gatt_svc_env_t;


static fmna_gatt_svc_env_t  s_fmna_gatt_svc_env;
static uint32_t s_acc_capability = 0;

static const uint8_t    s_ais_svc_uuid[]    = AIS_SERVICE_UUID;
static const uint8_t    s_fmna_svc_uuid[]   = BLE_ATT_16_TO_16_ARRAY(BLE_ATT_UUID_16(0xfd44));
#if FMNA_UARP_ENABLE
static const uint8_t    s_uarp_svc_uuid[]   = BLE_ATT_16_TO_16_ARRAY(BLE_ATT_UUID_16(0xfd43));
#endif


static sdk_err_t  accessory_info_service_init(void)
{
    s_fmna_gatt_svc_env.ais_char_mask = 0x7FFFF;
    memset(&s_fmna_gatt_svc_env.ais_gatts_db, 0, sizeof(ble_gatts_create_db_t));

    s_fmna_gatt_svc_env.ais_start_hdl                      = PRF_INVALID_HANDLE;
    s_fmna_gatt_svc_env.ais_gatts_db.shdl                  = &s_fmna_gatt_svc_env.ais_start_hdl;
    s_fmna_gatt_svc_env.ais_gatts_db.uuid                  = s_ais_svc_uuid;
    s_fmna_gatt_svc_env.ais_gatts_db.attr_tab_cfg          = (uint8_t *)&s_fmna_gatt_svc_env.ais_char_mask;
    s_fmna_gatt_svc_env.ais_gatts_db.max_nb_attr           = AIS_IDX_NB;
    s_fmna_gatt_svc_env.ais_gatts_db.srvc_perm             = BLE_GATTS_SRVC_UUID_TYPE_SET(BLE_GATTS_UUID_TYPE_128); 
    s_fmna_gatt_svc_env.ais_gatts_db.attr_tab_type         = BLE_GATTS_SERVICE_TABLE_TYPE_128;
    s_fmna_gatt_svc_env.ais_gatts_db.attr_tab.attr_tab_128 = ais_att_db;

    return ble_gatts_prf_add(&s_fmna_gatt_svc_env.ais_gatts_db, ais_ble_evt_handler);
}

static sdk_err_t  fmna_service_init(void)
{
    s_fmna_gatt_svc_env.fmna_char_mask = g_is_enable_debug ? 0xFFFF : 0x1FFF;
    memset(&s_fmna_gatt_svc_env.fmna_gatts_db, 0, sizeof(ble_gatts_create_db_t));

    s_fmna_gatt_svc_env.fmna_start_hdl                      = PRF_INVALID_HANDLE;
    s_fmna_gatt_svc_env.fmna_gatts_db.shdl                  = &s_fmna_gatt_svc_env.fmna_start_hdl;
    s_fmna_gatt_svc_env.fmna_gatts_db.uuid                  = s_fmna_svc_uuid;
    s_fmna_gatt_svc_env.fmna_gatts_db.attr_tab_cfg          = (uint8_t *)&s_fmna_gatt_svc_env.fmna_char_mask;
    s_fmna_gatt_svc_env.fmna_gatts_db.max_nb_attr           = g_is_enable_debug ? FMNA_IDX_NB : (FMNA_IDX_NB - 3);
    s_fmna_gatt_svc_env.fmna_gatts_db.srvc_perm             = 0; 
    s_fmna_gatt_svc_env.fmna_gatts_db.attr_tab_type         = BLE_GATTS_SERVICE_TABLE_TYPE_128;
    s_fmna_gatt_svc_env.fmna_gatts_db.attr_tab.attr_tab_128 = fmna_att_db;

    return ble_gatts_prf_add(&s_fmna_gatt_svc_env.fmna_gatts_db, fmna_ble_evt_handler);
}

extern int g_fmna_adv_tx_power;

static sdk_err_t tx_power_service_init(void)
{
    tps_init_t tps_env_init;

    tps_env_init.initial_tx_power_level = g_fmna_adv_tx_power;
    return tps_service_init(&tps_env_init);
}

#if FMNA_UARP_ENABLE
static sdk_err_t  uarp_service_init(void)
{
    s_fmna_gatt_svc_env.uarp_char_mask = 0xFFFF;
    memset(&s_fmna_gatt_svc_env.uarp_gatts_db, 0, sizeof(ble_gatts_create_db_t));

    s_fmna_gatt_svc_env.uarp_start_hdl                      = PRF_INVALID_HANDLE;
    s_fmna_gatt_svc_env.uarp_gatts_db.shdl                  = &s_fmna_gatt_svc_env.uarp_start_hdl;
    s_fmna_gatt_svc_env.uarp_gatts_db.uuid                  = s_uarp_svc_uuid;
    s_fmna_gatt_svc_env.uarp_gatts_db.attr_tab_cfg          = (uint8_t *)&s_fmna_gatt_svc_env.uarp_char_mask;
    s_fmna_gatt_svc_env.uarp_gatts_db.max_nb_attr           = UARP_IDX_NB;
    s_fmna_gatt_svc_env.uarp_gatts_db.srvc_perm             = 0; 
    s_fmna_gatt_svc_env.uarp_gatts_db.attr_tab_type         = BLE_GATTS_SERVICE_TABLE_TYPE_128;
    s_fmna_gatt_svc_env.uarp_gatts_db.attr_tab.attr_tab_128 = uarp_att_db;

    return ble_gatts_prf_add(&s_fmna_gatt_svc_env.uarp_gatts_db, uarp_ble_evt_handler);
}
#endif

void fmna_gatt_platform_mtu_update_handler(uint16_t status, uint16_t mtu)
{
    if (BLE_SUCCESS == status)
    {
        FMNA_LOG_DEBUG("GATT MTU exchanged: %d", mtu);
        m_gatt_mtu = mtu - 3;
    }
}

static void ais_read_att_evt_handler(uint8_t conn_idx, const ble_gatts_evt_read_t *p_param)
{
    ble_gatts_read_cfm_t   cfm;
    uint8_t                handle     = p_param->handle;
    uint8_t                tab_index  = 0;

    tab_index = prf_find_idx_by_handle(handle, s_fmna_gatt_svc_env.ais_start_hdl, AIS_IDX_NB, (uint8_t*)&s_fmna_gatt_svc_env.ais_char_mask);

    cfm.handle = handle;
    cfm.status = BLE_SUCCESS;

    switch(tab_index)
    {
        case AIS_IDX_PRODUCT_DATA_VAL:
        {
            cfm.length = FMNA_PRODUCT_DATA_LEN;
            cfm.value = g_fmna_info_cfg.product_data;
            break;
        }
        case AIS_IDX_MANUFA_NAME_VAL:
        {
            cfm.length = FMNA_MANUFACTURER_NAME_LEN;
            cfm.value = (uint8_t *)g_fmna_info_cfg.manufaturer_name_str;
            break;
        }
        case AIS_IDX_MODEL_NAME_VAL:
        {
            cfm.length = FMNA_MODEL_NAME_LEN;
            cfm.value  = (uint8_t *)g_fmna_info_cfg.model_name_str;
            break;
        }
        case AIS_IDX_ACCESS_CATE_VAL:
        {
            uint8_t accessory_category[FMNA_ACC_CATEGORY_LEN] = {0};
            memset(accessory_category, g_fmna_info_cfg.accessory_category, sizeof(uint8_t));
            cfm.length = FMNA_ACC_CATEGORY_LEN;
            cfm.value = accessory_category;
            break;
        }
        case AIS_IDX_ACCESS_CAPA_VAL:
        {
            s_acc_capability = 0;
            SET_BIT(s_acc_capability, ACC_CAPABILITY_PLAY_SOUND_BIT_POS);
            SET_BIT(s_acc_capability, ACC_CAPABILITY_SRNM_LOOKUP_BLE_BIT_POS);
            SET_BIT(s_acc_capability, ACC_CAPABILITY_UT_MOTION_DETECT_BIT_POS);
            #if FMNA_NFC_ENABLE
            if (!g_is_enable_nfc)
            {
                SET_BIT(s_acc_capability, ACC_CAPABILITY_SRNM_LOOKUP_NFC_BIT_POS);
            }
            #endif
            #if FMNA_UARP_ENABLE
            SET_BIT(s_acc_capability, ACC_CAPABILITY_FW_UPDATE_SERVICE_BIT_POS);
            #endif
            cfm.length = ACC_CAP_MAX_LEN;
            cfm.value = (uint8_t *)&s_acc_capability;
            break;
        }
        case AIS_IDX_FW_VERSION_VAL:
        {
            uint32_t fw_vers = fmna_version_get_fw_version();
            cfm.length = FW_VERS_MAX_LEN;
            cfm.value = (uint8_t *)&fw_vers;
            break;
        }
        case AIS_IDX_FMNA_VERSION_VAL:
        {
            uint32_t  findmy_vers = 0x00010000; // FindMy version 1.0.0
            cfm.length = FINDMY_VERS_MAX_LEN;
            cfm.value = (uint8_t *)&findmy_vers;
            break;
        }
        case AIS_IDX_BATTERY_TYPE_VAL:
        {
            uint8_t  batt_type = 0;
            cfm.length = BATT_TYPE_MAX_LEN;
            cfm.value = (uint8_t *)&batt_type;
            break;
        }
        case AIS_IDX_BATTERY_LEVEL_VAL:
        {
            uint8_t  batt_state = 0;
            cfm.length = BATT_LVL_MAX_LEN;
            cfm.value = (uint8_t *)&batt_state;
            break;
        }
        default:
            cfm.length = 0;
            cfm.status = BLE_ATT_ERR_INVALID_HANDLE;
            break;
    }

    ble_gatts_read_cfm(conn_idx,&cfm);
}

static void fmna_write_att_evt_handler(uint8_t conn_idx, const ble_gatts_evt_write_t *p_param)
{
    fmna_ret_code_t       ret_code = 0;
    uint8_t               tab_index;
    ble_gatts_write_cfm_t cfm;

    cfm.handle     = p_param->handle;
    cfm.status     = BLE_SUCCESS;

    tab_index = prf_find_idx_by_handle(p_param->handle, s_fmna_gatt_svc_env.fmna_start_hdl, FMNA_IDX_NB, (uint8_t *)&s_fmna_gatt_svc_env.fmna_char_mask);

    switch (tab_index)
    {
        case FMNA_IDX_PAIRING_CP_VAL:
            ret_code = fmna_gatt_pairing_char_authorized_write_handler(conn_idx, p_param->length, p_param->value);
            if (FMNA_ERROR_INVALID_STATE == ret_code)
            {
                cfm.status = BLE_ATT_ERR_WRITE_NOT_PERMITTED;
            }
            break;

        case FMNA_IDX_CONFIG_CP_VAL:
            fmna_gatt_config_char_write_handler(conn_idx, p_param->length, p_param->value);
            break;

        case FMNA_IDX_NON_OWNER_VAL:
            ret_code = fmna_gatt_nonown_char_write_handler(conn_idx, p_param->length, p_param->value);
            break;

        case FMNA_IDX_PAIRED_OWNER_INFO_CP_VAL:
            ret_code = fmna_gatt_paired_owner_char_write_handler(conn_idx, p_param->length, p_param->value);
            break;

#if FMNA_DEBUG_ENABLE
        case FMNA_IDX_DEBUG_CP_VAL:
            ret_code = fmna_gatt_debug_char_write_handler(conn_idx, p_param->length, p_param->value);
            break;
#endif
        default:
            cfm.status = BLE_ATT_ERR_INVALID_HANDLE;
            break;
    }

    FMNA_ERROR_CHECK(ret_code);

    ble_gatts_write_cfm(conn_idx, &cfm);
}


#if FMNA_UARP_ENABLE
static void uarp_write_att_evt_handler(uint8_t conn_idx, const ble_gatts_evt_write_t *p_param)
{
    fmna_ret_code_t   ret_code = 0;
    uint8_t               tab_index;
    ble_gatts_write_cfm_t cfm;

    cfm.handle     = p_param->handle;
    cfm.status     = BLE_SUCCESS;

    tab_index = prf_find_idx_by_handle(p_param->handle, s_fmna_gatt_svc_env.uarp_start_hdl, UARP_IDX_NB, (uint8_t *)&s_fmna_gatt_svc_env.uarp_char_mask);

    switch (tab_index)
    {
        case UARP_DATA_TRAN_VAL:
            fmna_uarp_connect(conn_idx);
            ret_code = fmna_uarp_authorized_rx_handler(conn_idx, p_param->value, p_param->length);
            if (ret_code == FMNA_ERROR_INVALID_STATE)
            {
                cfm.status = BLE_ATT_ERR_WRITE_NOT_PERMITTED;
            }
            break;

        default:
            cfm.status = BLE_ATT_ERR_INVALID_HANDLE;
            break;
    }

    ble_gatts_write_cfm(conn_idx, &cfm);
}

static void uarp_ntf_ind_evt_handler(uint8_t conn_idx, uint8_t status, const ble_gatts_evt_ntf_ind_t *p_ntf_ind)
{
    if (memcmp_val(&fmna_service_current_extended_packet_tx, 0, sizeof(fmna_service_current_extended_packet_tx)))
    {
        fmna_uarp_packet_sent();
    }
    else
    {
        fmna_gatt_dispatch_send_packet_extension_indication();
    }
}
#endif

static void fmna_ntf_ind_evt_handler(uint8_t conn_idx, uint8_t status, const ble_gatts_evt_ntf_ind_t *p_ntf_ind)
{
    if (!memcmp_val(&fmna_service_current_extended_packet_tx, 0, sizeof(fmna_service_current_extended_packet_tx)))
    {
        fmna_gatt_dispatch_send_packet_extension_indication();
    }
}


void fmna_gatt_platform_services_init(void)
{
    fmna_ret_code_t  err_code;

    memset(&s_fmna_gatt_svc_env, 0, sizeof(s_fmna_gatt_svc_env));

    err_code = accessory_info_service_init();
    FMNA_ERROR_CHECK(err_code);

    err_code = fmna_service_init();
    FMNA_ERROR_CHECK(err_code);

    err_code = tx_power_service_init();
    FMNA_ERROR_CHECK(err_code);

    #if FMNA_UARP_ENABLE
    err_code = uarp_service_init();
    FMNA_ERROR_CHECK(err_code);
    #endif
}

uint16_t fmna_gatt_platform_send_indication(uint16_t conn_handle, FMNA_Service_Opcode_t *p_opcode, uint8_t *p_data, uint16_t length)
{
    sdk_err_t            error_code = SDK_ERR_NTF_DISABLED;
    ble_gatts_noti_ind_t send_cmd;

    // If indication needs to be fragmented
    if (*p_opcode == FMNA_SERVICE_OPCODE_PACKET_EXTENSION)
    {
        *p_opcode = fmna_service_current_extended_packet_tx.opcode;
    }

    switch (*p_opcode & FMNA_SERVICE_OPCODE_BASE_MASK)
    {
        case FMNA_SERVICE_OPCODE_PAIRING_CONTROL_POINT_BASE:
            send_cmd.handle = prf_find_handle_by_idx(FMNA_IDX_PAIRING_CP_VAL, s_fmna_gatt_svc_env.fmna_start_hdl, (uint8_t *)&s_fmna_gatt_svc_env.fmna_char_mask);
            break;

        case FMNA_SERVICE_OPCODE_CONFIG_CONTROL_POINT_BASE:
            send_cmd.handle = prf_find_handle_by_idx(FMNA_IDX_CONFIG_CP_VAL, s_fmna_gatt_svc_env.fmna_start_hdl, (uint8_t *)&s_fmna_gatt_svc_env.fmna_char_mask);
            break;

        case FMNA_SERVICE_OPCODE_NON_OWNER_CONTROL_POINT_BASE:
            send_cmd.handle = prf_find_handle_by_idx(FMNA_IDX_NON_OWNER_VAL, s_fmna_gatt_svc_env.fmna_start_hdl, (uint8_t *)&s_fmna_gatt_svc_env.fmna_char_mask);
            break;

        case FMNA_SERVICE_OPCODE_PAIRED_OWNER_CONTROL_POINT_BASE:
            send_cmd.handle = prf_find_handle_by_idx(FMNA_IDX_PAIRED_OWNER_INFO_CP_VAL, s_fmna_gatt_svc_env.fmna_start_hdl, (uint8_t *)&s_fmna_gatt_svc_env.fmna_char_mask);
            break;

#if FMNA_DEBUG_ENABLE
        case FMNA_SERVICE_OPCODE_DEBUG_CONTROL_POINT_BASE:
            send_cmd.handle = prf_find_handle_by_idx(FMNA_IDX_DEBUG_CP_VAL, s_fmna_gatt_svc_env.fmna_start_hdl, (uint8_t *)&s_fmna_gatt_svc_env.fmna_char_mask);
            break;
#endif // DEBUG

#if FMNA_UARP_ENABLE
        case FMNA_SERVICE_OPCODE_INTERNAL_UARP_BASE:
            send_cmd.handle = prf_find_handle_by_idx(UARP_DATA_TRAN_VAL, s_fmna_gatt_svc_env.uarp_start_hdl, (uint8_t *)&s_fmna_gatt_svc_env.uarp_char_mask);
            break;
#endif

        default:
            FMNA_LOG_INFO("Unknown opcode: 0x%x", *p_opcode);
            break;
    }

    // Fill in the parameter structure
    send_cmd.type   = BLE_GATT_INDICATION;

    // Pack measured value in database
    send_cmd.length = length;
    send_cmd.value  = p_data;

    // Send notification to peer device
    error_code = ble_gatts_noti_ind(conn_handle, &send_cmd);

    return error_code;
}


static void ais_ble_evt_handler(const ble_evt_t *p_evt)
{
    if (NULL == p_evt)
    {
        return;
    }

    switch (p_evt->evt_id)
    {
        case BLE_GATTS_EVT_READ_REQUEST:
            ais_read_att_evt_handler(p_evt->evt.gattc_evt.index,&p_evt->evt.gatts_evt.params.read_req);
            break;
    }
}

static void fmna_ble_evt_handler(const ble_evt_t *p_evt)
{
    if (NULL == p_evt)
    {
        return;
    }

    switch (p_evt->evt_id)
    {
        case BLE_GATTS_EVT_WRITE_REQUEST:
            fmna_write_att_evt_handler(p_evt->evt.gattc_evt.index,&p_evt->evt.gatts_evt.params.write_req);
            break;

        case BLE_GATTS_EVT_NTF_IND:
            fmna_ntf_ind_evt_handler(p_evt->evt.gatts_evt.index, p_evt->evt_status, &p_evt->evt.gatts_evt.params.ntf_ind_sended);
            break;
    }
}
#if FMNA_UARP_ENABLE
static void uarp_ble_evt_handler(const ble_evt_t *p_evt)
{
    if (NULL == p_evt)
    {
        return;
    }

    switch (p_evt->evt_id)
    {
        case BLE_GATTS_EVT_WRITE_REQUEST:
            uarp_write_att_evt_handler(p_evt->evt.gattc_evt.index,&p_evt->evt.gatts_evt.params.write_req);
            break;

        case BLE_GATTS_EVT_NTF_IND:
            uarp_ntf_ind_evt_handler(p_evt->evt.gatts_evt.index, p_evt->evt_status, &p_evt->evt.gatts_evt.params.ntf_ind_sended);
            break;
    }
}
#endif
uint8_t fmna_gatt_platform_get_next_command_response_index(void)
{
    uint8_t index;
    GLOBAL_EXCEPTION_DISABLE();
    index = m_command_response_index;
    m_command_response_index++;
    if (m_command_response_index <= MAX_CONTROL_POINT_RSP)
    {
        m_command_response_index = 0;
    }
    GLOBAL_EXCEPTION_ENABLE();
    return index;
}

void fmna_application_gatt_service_hide(uint8_t conidx)
{
    extern uint16_t ble_gatts_service_hide_set(uint8_t conn_idx, uint16_t handle);
    extern uint16_t ble_gatts_service_hide_clear(uint8_t conn_idx);

    ble_gatts_service_hide_clear(conidx);
    ble_gatts_service_hide_set(conidx, s_fmna_gatt_svc_env.ais_start_hdl);
    ble_gatts_service_hide_set(conidx, s_fmna_gatt_svc_env.fmna_start_hdl);
    #if FMNA_UARP_ENABLE
    ble_gatts_service_hide_set(conidx, s_fmna_gatt_svc_env.uarp_start_hdl);
    #endif
    ble_gatts_service_hide_set(conidx, tps_service_start_handle_get());
}


