
#include "fmna_nfc.h"
#include "fmna_constants.h"
#include "fmna_connection.h"
#include "fmna_crypto.h"
#include "fmna_adv.h"
#include "fmna_version.h"


typedef struct
{
    char     bd_addr_str[FMNA_BLE_MAC_ADDR_BLEN * 2 + 1];
    char     srnm_raw[FMNA_SERIAL_NUMBER_LEN + 1];
} __attribute__((packed)) nfc_tap_url_unpaired_values_t;

typedef struct
{
    char srnm_encrypted[ENCRYPTED_SERIAL_NUMBER_PAYLOAD_BLEN * 2 + 1];
    char op[SERIAL_NUMBER_PAYLOAD_OP_BLEN];
} nfc_tap_url_paired_values_t;

typedef struct
{
    uint16_t pid;
    uint8_t  batt_status;
    uint32_t fw_version;
    union
    {
        nfc_tap_url_unpaired_values_t nfc_tap_url_unpaired_values;
        nfc_tap_url_paired_values_t   nfc_tap_url_paired_values;
    } nfc_tap_url_paired_state_values;
}nfc_tap_url_embedded_values_t;

static nfc_tap_url_embedded_values_t m_nfc_tap_url_embedded_values;

#define NFC_TAP_URL_MAX_BSIZE           (NFC_TAP_URL_BASE_BSIZE + sizeof(nfc_tap_url_embedded_values_t) + 50)

static const char *m_base_url            = "found.apple.com/accessory?pid=%04x&b=%02x&fv=%08x";
#define NFC_TAP_URL_BASE_BSIZE 51

static const char* m_unpaired_url_suffix = "&bt=%s&sr=%s";
static const char* m_paired_url_suffix   = "&e=%s&op=%s";

static uint8_t m_nfc_tap_url[NFC_TAP_URL_MAX_BSIZE];

static void hex_arr_to_ascii_str(char* dst_ascii_string, uint8_t* src_hex_arr, uint8_t data_length)
{
    for (uint8_t i = 0; i < data_length; i++) {
        dst_ascii_string[i] = (char)(src_hex_arr[i]);
    }
    dst_ascii_string[data_length] = '\0';
}

static void arr_to_hex_str(char* dst_hex_string, uint8_t *src_arr, uint8_t src_arr_length)
{
    char* dst_hex_string_start = dst_hex_string;
    for (uint8_t i = 0; i < src_arr_length; i++) {
        dst_hex_string += sprintf(dst_hex_string, "%02x", src_arr[i]);
    }
    *dst_hex_string = '\0';
    dst_hex_string = dst_hex_string_start;
}

/// Program the NFC URL with appropriate values.
static fmna_ret_code_t update_url(void)
 {
    // Clear the URL initially.
    memset(m_nfc_tap_url, 0, NFC_TAP_URL_MAX_BSIZE);

    int ret = snprintf((char *)m_nfc_tap_url,
                       NFC_TAP_URL_MAX_BSIZE,
                       m_base_url,
                       m_nfc_tap_url_embedded_values.pid,
                       m_nfc_tap_url_embedded_values.batt_status,
                       m_nfc_tap_url_embedded_values.fw_version);
    if (!(ret > 0 && ret < NFC_TAP_URL_MAX_BSIZE))
    {
        return FMNA_ERROR_INVALID_DATA;
    }

    uint8_t base_url_len = strlen((char *)m_nfc_tap_url);

    // Check if we are paired or not to use which URL.
    if (fmna_connection_is_fmna_paired())
    {
        ret = snprintf((char *)m_nfc_tap_url + base_url_len,
                       NFC_TAP_URL_MAX_BSIZE - base_url_len,
                       m_paired_url_suffix,
                       m_nfc_tap_url_embedded_values.nfc_tap_url_paired_state_values.nfc_tap_url_paired_values.srnm_encrypted,
                       m_nfc_tap_url_embedded_values.nfc_tap_url_paired_state_values.nfc_tap_url_paired_values.op);
    }
    else
    {
        ret = snprintf((char *)m_nfc_tap_url + base_url_len,
                       NFC_TAP_URL_MAX_BSIZE - base_url_len,
                       m_unpaired_url_suffix,
                       m_nfc_tap_url_embedded_values.nfc_tap_url_paired_state_values.nfc_tap_url_unpaired_values.bd_addr_str,
                       m_nfc_tap_url_embedded_values.nfc_tap_url_paired_state_values.nfc_tap_url_unpaired_values.srnm_raw);
    }

    if (!(ret > 0 && ret < (NFC_TAP_URL_MAX_BSIZE - base_url_len)))
    {
        return FMNA_ERROR_INVALID_DATA;
    }

    FMNA_LOG_DEBUG("NFC URL:%s, length:%d", m_nfc_tap_url, strlen((char *)m_nfc_tap_url));

    fmna_nfc_platform_info_update(m_nfc_tap_url, strlen((char *)m_nfc_tap_url));

    return FMNA_SUCCESS;
}

void fmna_nfc_set_url_key(FMNA_NFC_URL_Key_t nfc_url_key, void *nfc_url_key_data)
{
    if (!g_is_enable_nfc)
    {
        return;
    }

    fmna_ret_code_t ret_code;

    switch (nfc_url_key)
    {
        case URL_KEY_BATT_STATUS:
            FMNA_LOG_INFO("NFC update url key: [Battery Status]");
            m_nfc_tap_url_embedded_values.batt_status = *(uint8_t *)nfc_url_key_data;
            break;

        case URL_KEY_BT_MAC_ADDR:
            // Copy the pairing BD ADDR.
            FMNA_LOG_INFO("NFC update url key: [Pairing BD Address]");
            arr_to_hex_str(m_nfc_tap_url_embedded_values.nfc_tap_url_paired_state_values.nfc_tap_url_unpaired_values.bd_addr_str,
                           (uint8_t *)nfc_url_key_data,
                           FMNA_BLE_MAC_ADDR_BLEN);
            break;

        case URL_KEY_SERIAL_NUMBER_RAW:
            // Copy the SRNM.
            FMNA_LOG_INFO("NFC update url key: [Serial Number Raw]");
            hex_arr_to_ascii_str(m_nfc_tap_url_embedded_values.nfc_tap_url_paired_state_values.nfc_tap_url_unpaired_values.srnm_raw,
                                 (uint8_t *)nfc_url_key_data,
                                 FMNA_SERIAL_NUMBER_LEN);
            break;

        case URL_KEY_SERIAL_NUMBER_ENCRYPTED:
            FMNA_LOG_INFO("NFC update url key: [Serial Number Encrypted]");
            arr_to_hex_str(m_nfc_tap_url_embedded_values.nfc_tap_url_paired_state_values.nfc_tap_url_paired_values.srnm_encrypted,
                           (uint8_t *)nfc_url_key_data,
                           ENCRYPTED_SERIAL_NUMBER_PAYLOAD_BLEN);
        break;

        default:
            FMNA_LOG_WARNING("Unrecognized NFC Key: %d", nfc_url_key);
            break;
    }

    ret_code = update_url();
    FMNA_ERROR_CHECK(ret_code);
}

/// Generate next encrypted serial number and program the NFC URL.
static void update_nfc_encrypted_serial_number(void)
{
    // (re)Generate the encrypted serial number.
    fmna_ret_code_t ret_code = fmna_crypto_generate_serial_number_response(FMNA_SERIAL_NUMBER_QUERY_TYPE_TAP);
    FMNA_ERROR_CHECK(ret_code);

    // Set the encrypted serial number URL key for failure or success. In failure case,
    // the encrypted serial number will be cleared out.

    fmna_nfc_set_url_key(URL_KEY_SERIAL_NUMBER_ENCRYPTED, m_fmna_encrypted_serial_number_payload);
}

void fmna_nfc_load_paired_url(void)
{
    if (!g_is_enable_nfc)
    {
        return;
    }

    // Should be paired here. Assert if not.
    FMNA_BOOL_CHECK(fmna_connection_is_fmna_paired());

    strncpy(m_nfc_tap_url_embedded_values.nfc_tap_url_paired_state_values.nfc_tap_url_paired_values.op, "tap", SERIAL_NUMBER_PAYLOAD_OP_BLEN);

    // Generate the first encrypted serial number.
    update_nfc_encrypted_serial_number();
}

void fmna_nfc_load_unpaired_url(void)
{
    if (!g_is_enable_nfc)
    {
        return;
    }

    // Should be unpaired here.
    FMNA_BOOL_CHECK(!fmna_connection_is_fmna_paired());

    // Set the raw serial number, bluetooth address.

    uint8_t unpaired_bt_addr[FMNA_BLE_MAC_ADDR_BLEN];
    fmna_adv_get_unpaired_bt_addr(unpaired_bt_addr);

    // set address type bits for random static (0b11)
    unpaired_bt_addr[0] |= (uint8_t)FMNA_ADV_ADDR_TYPE_MASK;

    fmna_nfc_set_url_key(URL_KEY_BT_MAC_ADDR, unpaired_bt_addr);
    fmna_nfc_set_url_key(URL_KEY_SERIAL_NUMBER_RAW, fmna_crypto_get_serial_number_raw());
}

void fmna_nfc_init(void) {
    if (!g_is_enable_nfc)
    {
        return;
    }

    fmna_ret_code_t ret_code;

    m_nfc_tap_url_embedded_values.pid = g_accessory_pid;

    // Initialize base URL keys.
    m_nfc_tap_url_embedded_values.fw_version = fmna_version_get_fw_version();

    ret_code = update_url();
    FMNA_ERROR_CHECK(ret_code);

}

void fmna_nfc_field_off(void)
{
    // Generate the next encrypted serial number val.
    update_nfc_encrypted_serial_number();
}
