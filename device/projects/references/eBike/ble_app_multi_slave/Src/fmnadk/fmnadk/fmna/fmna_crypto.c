/*
 *      Copyright (C) 2020 Apple Inc. All Rights Reserved.
 *
 *      Find My Network ADK is licensed under Apple Inc.’s MFi Sample Code License Agreement,
 *      which is contained in the License.txt file distributed with the Find My Network ADK,
 *      and only to those who accept that license.
 */

#include "fmna_constants.h"


#include "fmna_crypto.h"
#include "fmna_state_machine.h"
#include "fmna_connection.h"
#include "fmna_nfc.h"
#include "fmna_version.h"
#include "fm-crypto.h"


fmna_send_pairing_data_t        m_fmna_send_pairing_data                                                     = {0};
fmna_initiate_pairing_data_t    m_fmna_initiate_pairing_data                                                 = {0};
fmna_finalize_pairing_data_t    m_fmna_finalize_pairing_data                                                 = {0};
fmna_send_pairing_status_t      m_fmna_send_pairing_status                                                   = {0};
uint8_t                         m_fmna_encrypted_serial_number_payload[ENCRYPTED_SERIAL_NUMBER_PAYLOAD_BLEN] = {0};

static uint8_t m_serial_number[FMNA_SERIAL_NUMBER_LEN];






typedef struct {
    uint8_t  serial_number[FMNA_SERIAL_NUMBER_LEN];
    uint64_t counter;
    char     op[SERIAL_NUMBER_PAYLOAD_OP_BLEN];
} __attribute__((packed)) serial_number_hmac_payload_t;

static serial_number_hmac_payload_t m_serial_number_hmac_payload;

typedef struct {
    uint8_t  serial_number[FMNA_SERIAL_NUMBER_LEN];
    uint64_t counter;
    uint8_t  hmac[SERIAL_NUMBER_PAYLOAD_HMAC_BLEN];
    char     op[SERIAL_NUMBER_PAYLOAD_OP_BLEN];
} __attribute__((packed)) serial_number_payload_t;

static serial_number_payload_t m_serial_number_payload;

static struct fm_crypto_ckg_context m_fm_crypto_ckg_ctx;

static uint8_t m_seedk1[SK_BLEN];
static uint8_t m_p[P_BLEN];
static uint8_t m_server_shared_secret[SERVER_SHARED_SECRET_BLEN];
static uint64_t serial_number_query_count = 0;

// Hardcoded values, eventually read these on boot from provisioned keys;
static uint8_t m_q_e[APPLE_SERVER_ENCRYPTION_KEY_BLEN] = {0x04, 0x9c, 0xc5, 0xad, 0xdd, 0xd0, 0x29, 0xb7, 0x53, 0x5d, 0x30, 0xe6, 0xe5, 0xd1, 0x6d, 0xb7, 0xa8, 0xd2, 0x1b, 0x1b, 0x48, 0xb5, 0x5b, 0x19, 0xd5, 0xb1, 0x10, 0xe9, 0x5b, 0xf3, 0x15, 0x45, 0xe7, 0x74, 0xcf, 0x51, 0x8d, 0xeb, 0xbe, 0x3c, 0x71, 0x68, 0x33, 0xe4, 0x43, 0xf1, 0x14, 0x47, 0x6e, 0x5a, 0x4b, 0x05, 0x4e, 0x36, 0x75, 0x07, 0x05, 0x6e, 0x39, 0x95, 0xcc, 0x6b, 0x96, 0x90, 0x96};
static uint8_t m_q_a[APPLE_SERVER_SIG_VERIFICATION_KEY_BLEN] = {0x04, 0x33, 0x4c, 0x5a, 0x73, 0xfd, 0x61, 0xdf, 0x36, 0x43, 0x3f, 0xbc, 0x69, 0x92, 0x36, 0xe3, 0x98, 0xe4, 0x94, 0x12, 0xf3, 0xc0, 0xfd, 0xc4, 0xe5, 0xda, 0x0b, 0x41, 0x18, 0x77, 0x95, 0x17, 0x08, 0x71, 0x20, 0x88, 0x8e, 0x97, 0x92, 0x37, 0x76, 0xba, 0x48, 0xdc, 0x51, 0x7c, 0x0f, 0xa8, 0x7b, 0x9c, 0x62, 0xa9, 0xfe, 0xe9, 0x6b, 0x0f, 0x38, 0x40, 0x3f, 0x66, 0x9e, 0x1e, 0x67, 0x55, 0x60};

typedef struct {
    uint8_t m_software_auth_uuid[SOFTWARE_AUTH_UUID_BLEN];
    char m_software_auth_token[SOFTWARE_AUTH_TOKEN_BLEN];
} mfi_info_t;

static mfi_info_t m_mfi_struct;

typedef struct {
    uint8_t  session_nonce[SESSION_NONCE_BLEN];
    char     software_auth_token[SOFTWARE_AUTH_TOKEN_BLEN];
    uint8_t  software_auth_uuid[SOFTWARE_AUTH_UUID_BLEN];
    uint8_t  serial_number[FMNA_SERIAL_NUMBER_LEN];
    uint8_t  product_data[FMNA_PRODUCT_DATA_LEN];
    uint32_t fw_version;
    uint8_t  e1[E1_BLEN];
    uint8_t  seedk1[SK_BLEN];
} __attribute__((packed)) e2_generation_encryption_msg_t;

typedef struct {
    uint8_t  software_auth_uuid[SOFTWARE_AUTH_UUID_BLEN];
    uint8_t  serial_number[FMNA_SERIAL_NUMBER_LEN];
    uint8_t  session_nonce[SESSION_NONCE_BLEN];
    uint8_t  e1[E1_BLEN];
    uint8_t  latest_sw_token[SOFTWARE_AUTH_TOKEN_BLEN];
    uint32_t status;
} __attribute__((packed)) e4_generation_encryption_msg_t;

typedef struct {
    uint8_t  software_auth_uuid[SOFTWARE_AUTH_UUID_BLEN];
    uint8_t  session_nonce[SESSION_NONCE_BLEN];
    uint8_t  seeds[SEEDS_BLEN];
    uint8_t  h1[H1_BLEN];
    uint8_t  e1[E1_BLEN];
    uint8_t  e3[E3_BLEN];
} __attribute__((packed)) s2_verification_msg_t;

// Union to hold buffers for various encryption, verification messages.
typedef union {
    e2_generation_encryption_msg_t e2_generation_encryption_msg;
    e4_generation_encryption_msg_t e4_generation_encryption_msg;
    s2_verification_msg_t          s2_verification_msg;
} key_verif_encr_msg_t;

e2_generation_encryption_msg_t decrypted_e2_generation_encryption_msg;

static key_verif_encr_msg_t m_key_verif_encr_msg;

static uint8_t m_current_primary_sk[SK_BLEN];
static uint8_t m_current_secondary_sk[SK_BLEN];

#define FM_CRYPTO_STATUS_SUCCESS 0

static void populate_e2_generation_encryption_msg(void) {
    // Populate the fields for m_key_verif_encr_msg.e2_generation_encryption_msg.

    memcpy(m_key_verif_encr_msg.e2_generation_encryption_msg.session_nonce,
           m_fmna_initiate_pairing_data.session_nonce,
           SESSION_NONCE_BLEN);
    
    memcpy(m_key_verif_encr_msg.e2_generation_encryption_msg.software_auth_uuid,
           m_mfi_struct.m_software_auth_uuid,
           SOFTWARE_AUTH_UUID_BLEN);
    
    memcpy(m_key_verif_encr_msg.e2_generation_encryption_msg.software_auth_token,
           m_mfi_struct.m_software_auth_token,
           SOFTWARE_AUTH_TOKEN_BLEN);
    
    memcpy(m_key_verif_encr_msg.e2_generation_encryption_msg.serial_number,
           m_serial_number,
           FMNA_SERIAL_NUMBER_LEN);
    
    memcpy(m_key_verif_encr_msg.e2_generation_encryption_msg.e1,
           m_fmna_initiate_pairing_data.e1,
           E1_BLEN);
    
    memcpy(m_key_verif_encr_msg.e2_generation_encryption_msg.seedk1,
           m_seedk1,
           SK_BLEN);
    
    m_key_verif_encr_msg.e2_generation_encryption_msg.fw_version = fmna_version_get_fw_version();

    memcpy(m_key_verif_encr_msg.e2_generation_encryption_msg.product_data,
           g_fmna_info_cfg.product_data,
           FMNA_PRODUCT_DATA_LEN);
}

static void populate_e4_generation_encryption_msg(void) {
    // Populate the fields for m_key_verif_encr_msg.e4_generation_encryption_msg.
    
    memcpy(m_key_verif_encr_msg.e4_generation_encryption_msg.session_nonce,
           m_fmna_initiate_pairing_data.session_nonce,
           SESSION_NONCE_BLEN);
    
    memcpy(m_key_verif_encr_msg.e4_generation_encryption_msg.software_auth_uuid,
           m_mfi_struct.m_software_auth_uuid,
           SOFTWARE_AUTH_UUID_BLEN);
    
    memcpy(m_key_verif_encr_msg.e4_generation_encryption_msg.serial_number,
           m_serial_number,
           FMNA_SERIAL_NUMBER_LEN);
    
    memcpy(m_key_verif_encr_msg.e4_generation_encryption_msg.e1,
           m_fmna_initiate_pairing_data.e1,
           E1_BLEN);
    
    memcpy(m_key_verif_encr_msg.e4_generation_encryption_msg.latest_sw_token,
           m_mfi_struct.m_software_auth_token,
           SOFTWARE_AUTH_TOKEN_BLEN);
    
    m_key_verif_encr_msg.e4_generation_encryption_msg.status = 0;
}

static void populate_s2_verification_msg(void) {
    // Populate the fields for m_key_verif_encr_msg.s2_verification_msg.
    
    memcpy(m_key_verif_encr_msg.s2_verification_msg.session_nonce,
           m_fmna_initiate_pairing_data.session_nonce,
           SESSION_NONCE_BLEN);
    
    memcpy(m_key_verif_encr_msg.s2_verification_msg.software_auth_uuid,
           m_mfi_struct.m_software_auth_uuid,
           SOFTWARE_AUTH_UUID_BLEN);
    
    memcpy(m_key_verif_encr_msg.s2_verification_msg.seeds,
           m_fmna_finalize_pairing_data.seeds,
           SEEDS_BLEN);
    
    int ret = fm_crypto_sha256(C2_BLEN, m_fmna_finalize_pairing_data.c2, m_key_verif_encr_msg.s2_verification_msg.h1);
    FMNA_ERROR_CHECK(ret);
    
    memcpy(m_key_verif_encr_msg.s2_verification_msg.e1,
           m_fmna_initiate_pairing_data.e1,
           E1_BLEN);
    
    memcpy(m_key_verif_encr_msg.s2_verification_msg.e3,
           m_fmna_finalize_pairing_data.e3,
           E3_BLEN);
}

static fmna_ret_code_t roll_sk(uint8_t current_sk[SK_BLEN]) {
    uint8_t new_sk[SK_BLEN];
    int ret_code = fm_crypto_roll_sk(current_sk, new_sk);
    if (ret_code != FM_CRYPTO_STATUS_SUCCESS) {
        FMNA_ERROR_CHECK(ret_code);
        return FMNA_ERROR_INTERNAL;
    }
    memcpy(current_sk, new_sk, SK_BLEN);
    
    return FMNA_SUCCESS;
}


uint8_t *fmna_crypto_get_serial_number_raw(void) {
    return m_serial_number;
}


static void fmna_crypto_keys_load(void)
{
    uint16_t ret;

    if (fmna_connection_is_fmna_paired())
    {
        FMNA_LOG_INFO("FMNA Crypoto Information Load...");
        uint16_t len = P_BLEN;

        ret = fmna_storage_platform_key_value_get(FMNA_PUBLIC_KEY_P_NV_TAG, &len, (uint8_t *)&m_p);
        FMNA_ERROR_CHECK(ret);

        len = SK_BLEN;
        ret = fmna_storage_platform_key_value_get(FMNA_SYMMETRIC_KEY_SKN_NV_TAG, &len, (uint8_t *)&m_current_primary_sk);
        FMNA_ERROR_CHECK(ret);

        len = SK_BLEN;
        ret = fmna_storage_platform_key_value_get(FMNA_SYMMETRIC_KEY_SKS_NV_TAG, &len, (uint8_t *)&m_current_secondary_sk);
        FMNA_ERROR_CHECK(ret);

        len = SERVER_SHARED_SECRET_BLEN;
        ret = fmna_storage_platform_key_value_get(FMNA_SERVER_SHARED_SECRET_NV_TAG, &len, (uint8_t *)&m_server_shared_secret);
        FMNA_ERROR_CHECK(ret);

        len = sizeof(m_fmna_current_primary_key);
        ret = fmna_storage_platform_key_value_get(FMNA_CURRENT_PRIMARY_KEY_NV_TAG, &len, (uint8_t *)&m_fmna_current_primary_key);
        FMNA_ERROR_CHECK(ret);

        len = sizeof(m_fmna_current_secondary_key);
        ret = fmna_storage_platform_key_value_get(FMNA_CURRENT_SECONDARY_KEY_NV_TAG, &len, (uint8_t *)&m_fmna_current_secondary_key);
        FMNA_ERROR_CHECK(ret);

        len = ICLOUD_IDENTIFIER_BLEN;
        ret = fmna_storage_platform_key_value_get(FMNA_ICLOUD_IDENTIFIER_NV_TAG, &len, m_fmna_finalize_pairing_data.icloud_id);
        FMNA_ERROR_CHECK(ret);
    }
}

void fmna_crypto_init(void)
{
    fmna_crypto_keys_load();

    int ret = fm_crypto_ckg_init(&m_fm_crypto_ckg_ctx);
    FMNA_ERROR_CHECK(ret);

    //Read software auth token, UUID, serial number from accessory factory registers/ flash.
    fmna_storage_platform_flash_read(g_fmna_info_cfg.software_auth_uuid_save_addr + SOFTWARE_AUTH_UUID_BLEN, (uint8_t *)m_mfi_struct.m_software_auth_token, SOFTWARE_AUTH_TOKEN_BLEN);
    FMNA_LOG_DEBUG("Software auth token preview:");
    FMNA_LOG_HEXDUMP_DEBUG((uint8_t *)m_mfi_struct.m_software_auth_token, 16);

    fmna_storage_platform_flash_read(g_fmna_info_cfg.software_auth_uuid_save_addr, m_mfi_struct.m_software_auth_uuid, SOFTWARE_AUTH_UUID_BLEN);
    FMNA_LOG_DEBUG("Software auth uuid preview:");
    FMNA_LOG_HEXDUMP_DEBUG(m_mfi_struct.m_software_auth_uuid, 16);

    fmna_connection_platform_get_serial_number(m_serial_number, FMNA_SERIAL_NUMBER_LEN);
}

fmna_ret_code_t fmna_crypto_generate_send_pairing_data_params(void) {
    // Generate C1, SeedK1, and E2.
    int ret = fm_crypto_ckg_gen_c1(&m_fm_crypto_ckg_ctx, m_fmna_send_pairing_data.c1);
    if (FM_CRYPTO_STATUS_SUCCESS != ret) {
        FMNA_ERROR_CHECK(ret);
        return FMNA_ERROR_INTERNAL;
    }
    
    ret = fm_crypto_generate_seedk1(m_seedk1);
    if (FM_CRYPTO_STATUS_SUCCESS != ret) {
        FMNA_ERROR_CHECK(ret);
        return FMNA_ERROR_INTERNAL;
    }
    
    //Generate E2, see Section 6.2.3 for details.
    
    populate_e2_generation_encryption_msg();
    FMNA_LOG_DEBUG("E2 generation encryption msg size %d", sizeof(m_key_verif_encr_msg.e2_generation_encryption_msg));
    uint32_t e2_blen = E2_BLEN;
    ret = fm_crypto_encrypt_to_server((const uint8_t *)m_q_e,
                                      sizeof(m_key_verif_encr_msg.e2_generation_encryption_msg),
                                      (const uint8_t *)&m_key_verif_encr_msg.e2_generation_encryption_msg,
                                      (unsigned int *)&e2_blen,
                                      m_fmna_send_pairing_data.e2);
    if (FM_CRYPTO_STATUS_SUCCESS != ret) {
        FMNA_ERROR_CHECK(ret);
        return FMNA_ERROR_INTERNAL;
    }
    FMNA_LOG_DEBUG("E2, len %lu", e2_blen);
    return FMNA_SUCCESS;
}

fmna_ret_code_t fmna_crypto_finalize_pairing(void) {
    // Validate S2, decrypt E3, generate C3, generate E4, and send response.

    int ret = fm_crypto_derive_server_shared_secret(m_fmna_finalize_pairing_data.seeds, m_seedk1, m_server_shared_secret);
    if (FM_CRYPTO_STATUS_SUCCESS != ret) {
        FMNA_ERROR_CHECK(ret);
        return FMNA_ERROR_INTERNAL;
    }
    fmna_storage_platform_key_value_set(FMNA_SERVER_SHARED_SECRET_NV_TAG, SERVER_SHARED_SECRET_BLEN, (uint8_t *)&m_server_shared_secret);

    populate_s2_verification_msg();
    FMNA_LOG_DEBUG("S2 verification msg len %d", sizeof(m_key_verif_encr_msg.s2_verification_msg));
    ret = fm_crypto_verify_s2(m_q_a,
                              S2_BLEN,
                              m_fmna_finalize_pairing_data.s2,
                              sizeof(m_key_verif_encr_msg.s2_verification_msg),
                              (const uint8_t *)(&m_key_verif_encr_msg.s2_verification_msg));
    if (FM_CRYPTO_STATUS_SUCCESS != ret) {
        FMNA_ERROR_CHECK(ret);
        return FMNA_ERROR_INTERNAL;
    }
    
    uint32_t e3_decrypt_plaintext_blen = SOFTWARE_AUTH_TOKEN_BLEN;
    ret = fm_crypto_decrypt_e3((const uint8_t *)m_server_shared_secret,
                               E3_BLEN,
                               (const uint8_t *)m_fmna_finalize_pairing_data.e3,
                               (unsigned int *)&e3_decrypt_plaintext_blen,
                               (uint8_t *)m_mfi_struct.m_software_auth_token);
    if (FM_CRYPTO_STATUS_SUCCESS != ret) {
        FMNA_ERROR_CHECK(ret);
        return FMNA_ERROR_INTERNAL;
    }
    FMNA_LOG_DEBUG("E3 decrypted token len %d", e3_decrypt_plaintext_blen);
    fmna_log_mfi_token();
    
    ret = fm_crypto_ckg_gen_c3(&m_fm_crypto_ckg_ctx,
                               m_fmna_finalize_pairing_data.c2,
                               m_fmna_send_pairing_status.c3);
    if (FM_CRYPTO_STATUS_SUCCESS != ret) {
        return FMNA_ERROR_INTERNAL;
    }
    
    
    // Generate E4, see Section 6.2.6 for details.
    populate_e4_generation_encryption_msg();
    FMNA_LOG_DEBUG("E4 generation encryption msg size %d", sizeof(m_key_verif_encr_msg.e4_generation_encryption_msg));
    uint32_t e4_blen = E4_BLEN;
    ret = fm_crypto_encrypt_to_server((const uint8_t *)m_q_e,
                                      sizeof(m_key_verif_encr_msg.e4_generation_encryption_msg),
                                      (const uint8_t *)&m_key_verif_encr_msg.e4_generation_encryption_msg,
                                      (unsigned int *)&e4_blen,
                                      m_fmna_send_pairing_status.e4);
    if (FM_CRYPTO_STATUS_SUCCESS != ret) {
        FMNA_ERROR_CHECK(ret);
        return FMNA_ERROR_INTERNAL;
    }
    FMNA_LOG_DEBUG("E4, len %lu", e4_blen);

    fmna_connection_update_mfi_token_storage(&m_mfi_struct, sizeof(m_mfi_struct));
    
    return FMNA_SUCCESS;
}

fmna_ret_code_t fmna_crypto_pairing_complete(void) {
    int ret = fm_crypto_ckg_finish(&m_fm_crypto_ckg_ctx,
                                   m_p,
                                   m_current_primary_sk,
                                   m_current_secondary_sk);
    if (FM_CRYPTO_STATUS_SUCCESS != ret) {
        FMNA_ERROR_CHECK(ret);
        return FMNA_ERROR_INTERNAL;
    }

    FMNA_LOG_DEBUG("P:");
    FMNA_LOG_HEXDUMP_DEBUG(m_p, P_BLEN);
    fmna_storage_platform_key_value_set(FMNA_PUBLIC_KEY_P_NV_TAG, P_BLEN, (uint8_t *)&m_p);

    FMNA_LOG_DEBUG("Primary SKN:");
    FMNA_LOG_HEXDUMP_DEBUG(m_current_primary_sk, SK_BLEN);
    fmna_storage_platform_key_value_set(FMNA_SYMMETRIC_KEY_SKN_NV_TAG, SK_BLEN, (uint8_t *)&m_current_primary_sk);

    FMNA_LOG_DEBUG("Secondary SKN:");
    FMNA_LOG_HEXDUMP_DEBUG(m_current_secondary_sk, SK_BLEN);
    fmna_storage_platform_key_value_set(FMNA_SYMMETRIC_KEY_SKS_NV_TAG, SK_BLEN, (uint8_t *)&m_current_secondary_sk);

    fm_crypto_ckg_free(&m_fm_crypto_ckg_ctx);

    return FMNA_SUCCESS;
}

fmna_ret_code_t fmna_crypto_roll_primary_sk(void) {
    return roll_sk(m_current_primary_sk);
}

fmna_ret_code_t fmna_crypto_roll_secondary_sk(void) {
    return roll_sk(m_current_secondary_sk);
}

fmna_ret_code_t fmna_crypto_roll_primary_key(void) {
    // SK(i) -> SK(i+1)
    fmna_ret_code_t fmna_ret_code = fmna_crypto_roll_primary_sk();
    if (fmna_ret_code != FMNA_SUCCESS) {
        return fmna_ret_code;
    }
    
    // SK(i+1) -> Primary_Key(i+1)
    int ret = fm_crypto_derive_primary_or_secondary_x(m_current_primary_sk, m_p, m_fmna_current_primary_key.public_key);
    if (FM_CRYPTO_STATUS_SUCCESS != ret) {
        FMNA_ERROR_CHECK(ret);
        return FMNA_ERROR_INTERNAL;
    }
    
    // Increment Primary Key index
    m_fmna_current_primary_key.index++;

    FMNA_LOG_DEBUG("Current Primary Key (index = 0x%x):", m_fmna_current_primary_key.index);
    FMNA_LOG_HEXDUMP_DEBUG(m_fmna_current_primary_key.public_key, FMNA_PUBKEY_BLEN);
    
    // SK(i+1) -> LTK(i+1)
    ret = fm_crypto_derive_ltk(m_current_primary_sk, m_fmna_current_primary_key.ltk);
    if (FM_CRYPTO_STATUS_SUCCESS != ret) {
        FMNA_BOOL_CHECK(ret);
        return FMNA_ERROR_INTERNAL;
    }

    FMNA_LOG_DEBUG("Current LTK:");
    FMNA_LOG_HEXDUMP_DEBUG(m_fmna_current_primary_key.ltk, FMNA_BLE_SEC_KEY_SIZE);
    fmna_connection_set_active_ltk(m_fmna_current_primary_key.ltk);
    fmna_storage_platform_key_value_set(FMNA_SYMMETRIC_KEY_SKN_NV_TAG, SK_BLEN, (uint8_t *)&m_current_primary_sk);
    fmna_storage_platform_key_value_set(FMNA_CURRENT_PRIMARY_KEY_NV_TAG, sizeof(m_fmna_current_primary_key), (uint8_t *)&m_fmna_current_primary_key);

    return FMNA_SUCCESS;
}

fmna_ret_code_t fmna_crypto_roll_secondary_key(void) {
    // SK(i) -> SK(i+1)
    fmna_ret_code_t fmna_ret_code = fmna_crypto_roll_secondary_sk();
    if (fmna_ret_code != FMNA_SUCCESS) {
        return fmna_ret_code;
    }

    // SK(i+1) -> Secondary_Key(i+1)
    int ret = fm_crypto_derive_primary_or_secondary_x(m_current_secondary_sk, m_p, m_fmna_current_secondary_key.public_key);
    if (FM_CRYPTO_STATUS_SUCCESS != ret) {
        FMNA_ERROR_CHECK(ret);
        return FMNA_ERROR_INTERNAL;
    }

    // Increment Secondary Key index
    m_fmna_current_secondary_key.index++;

    FMNA_LOG_DEBUG("Curr Secondary Key (index = 0x%x):", m_fmna_current_secondary_key.index);
    FMNA_LOG_HEXDUMP_DEBUG(m_fmna_current_secondary_key.public_key, FMNA_PUBKEY_BLEN);
    fmna_storage_platform_key_value_set(FMNA_SYMMETRIC_KEY_SKS_NV_TAG, SK_BLEN, (uint8_t *)&m_current_secondary_sk);
    fmna_storage_platform_key_value_set(FMNA_CURRENT_SECONDARY_KEY_NV_TAG, sizeof(m_fmna_current_secondary_key), (uint8_t *)&m_fmna_current_secondary_key);
    return FMNA_SUCCESS;
}

fmna_ret_code_t fmna_crypto_generate_serial_number_response(FMNA_Serial_Number_Query_Type_t type) {
    int ret;
    
    // Clear the encrypted serial number initially in case of error.
    memset(m_fmna_encrypted_serial_number_payload, 0, ENCRYPTED_SERIAL_NUMBER_PAYLOAD_BLEN);
    
    serial_number_query_count++;
    FMNA_LOG_DEBUG("Serial number query count: %ld", (uint32_t)serial_number_query_count);
    
    memcpy(m_serial_number_hmac_payload.serial_number, m_serial_number, FMNA_SERIAL_NUMBER_LEN);
    memcpy(m_serial_number_payload.serial_number, m_serial_number, FMNA_SERIAL_NUMBER_LEN);
    
    m_serial_number_hmac_payload.counter = serial_number_query_count;
    m_serial_number_payload.counter      = serial_number_query_count;
    
    switch (type) {
        case FMNA_SERIAL_NUMBER_QUERY_TYPE_TAP:
            strcpy(m_serial_number_hmac_payload.op, "tap");
            strcpy(m_serial_number_payload.op, "tap");
            break;
        case FMNA_SERIAL_NUMBER_QUERY_TYPE_BT:
            strcpy(m_serial_number_hmac_payload.op, "bt");
            strcpy(m_serial_number_payload.op, "bt");
            break;
        default:
            return FMNA_ERROR_INTERNAL;
    }
    
    ret = fm_crypto_authenticate_with_ksn(m_server_shared_secret,
                                          sizeof(m_serial_number_hmac_payload),
                                          (const uint8_t *)&m_serial_number_hmac_payload,
                                          m_serial_number_payload.hmac);
    if (ret != FM_CRYPTO_STATUS_SUCCESS) {
        FMNA_ERROR_CHECK(ret);
        return FMNA_ERROR_INTERNAL;
    }
    
    uint32_t encrypted_serial_number_payload_blen = ENCRYPTED_SERIAL_NUMBER_PAYLOAD_BLEN;
    ret = fm_crypto_encrypt_to_server(m_q_e,
                                      sizeof(m_serial_number_payload),
                                      (const uint8_t *)&m_serial_number_payload,
                                      (unsigned int *)&encrypted_serial_number_payload_blen,
                                      m_fmna_encrypted_serial_number_payload);
    if (ret != FM_CRYPTO_STATUS_SUCCESS) {
        FMNA_ERROR_CHECK(ret);
        
        // Clear the encrypted serial number in case of fm_crypto_encrypt_to_server error. 
        memset(m_fmna_encrypted_serial_number_payload, 0, ENCRYPTED_SERIAL_NUMBER_PAYLOAD_BLEN);
        return FMNA_ERROR_INTERNAL;
    }
    return FMNA_SUCCESS;
}

void fmna_crypto_unpair(void) {
    FMNA_LOG_DEBUG("Initialize new CKG context");

    // Initialize new CKG context on unpair in preparation for new pairing.
    int ret = fm_crypto_ckg_init(&m_fm_crypto_ckg_ctx);
    FMNA_ERROR_CHECK(ret);
}

void fmna_log_mfi_token(void) {
    fmna_connection_platform_log_token(m_mfi_struct.m_software_auth_token, SOFTWARE_AUTH_TOKEN_BLEN, 0);
}

