/*
 *      Copyright (C) 2020 Apple Inc. All Rights Reserved.
 *
 *      Find My Network ADK is licensed under Apple Inc.’s MFi Sample Code License Agreement,
 *      which is contained in the License.txt file distributed with the Find My Network ADK,
 *      and only to those who accept that license.
 */

#ifndef fmna_constants_h
#define fmna_constants_h

#define FMNA_COMPANY_IDENTIFIER                        0x004C                                      /**< Company identifier for Apple. as per www.bluetooth.org. */

#define SYS_POINTER_BSIZE                             (sizeof(void *))

#define FMNA_PUBKEY_BLEN                              28                                          /**< Length (in bytes) of the public key. */
#define FMNA_SEPARATED_ADV_PAYLOAD_PUBKEY_BLEN        (FMNA_PUBKEY_BLEN - 6)                      /**< Length (in bytes) of the Separated payload public key (bytes 6-27). */
#define FMNA_SEPARATED_ADV_PUBKEY_HINT_INDEX          5
#define PRIMARY_KEYS_PER_SECONDARY_KEY                96
#define SEPARATED_STATE_CONFIG_NEGATIVE_BOUNDARY      4
#define FMNA_BLE_MAC_ADDR_BLEN                        6                                           /** 6 bytes per BT spec. */
#define NEARBY_TIMEOUT_MAX_SECONDS                    3600
#define FMNA_SERIAL_NUMBER_REPORT_TIMER               300000                                      /** 5 minutes,ms for unit. */
#define FMNA_PAIR_MODE_TIMEOUT                        600000                                     /** 10 minutes,ms for unit. */

#define NUM_OF_KEYS                                   8
#define PAIRING_MAX_LEN                               1394
#define CONFIG_MAX_LEN                                64
#define NONOWN_MAX_LEN                                7
#define PAIRED_OWNER_MAX_LEN                          144

//MARK: Apple Information Service lengths
#define RESERVED_MAX_LEN                              3
#define ACC_CAP_MAX_LEN                               4
#define FW_VERS_MAX_LEN                               4
#define FINDMY_VERS_MAX_LEN                           4
#define BATT_TYPE_MAX_LEN                             1
#define BATT_LVL_MAX_LEN                              1

// Operand Lengths as per spec - R2 update from 8/5/20


#define INITIATE_PAIRING_DATA_LENGTH                  (SESSION_NONCE_BLEN + E1_BLEN)

#define OPCODE_OP_LENGTH                              4
#define STATUS_LENGTH                                 4

//MARK: Crypto Key Lengths
#define SESSION_NONCE_BLEN                            32
#define SERVER_SHARED_SECRET_BLEN                     32
#define SOFTWARE_AUTH_TOKEN_BLEN                      1024
#define SOFTWARE_AUTH_UUID_BLEN                       16
#define SK_BLEN                                       32
#define P_BLEN                                        57
#define C1_BLEN                                       32
#define E1_BLEN                                       113
#define C3_BLEN                                       60
#define E2_BLEN                                       1326
#define E3_BLEN                                       1040
#define E4_BLEN                                       1286
#define C2_BLEN                                       89
#define SEEDS_BLEN                                    32
#define ICLOUD_IDENTIFIER_BLEN                        60
#define S2_BLEN                                       100
#define H1_BLEN                                       32
#define SERIAL_NUMBER_PAYLOAD_HMAC_BLEN               32
#define SERIAL_NUMBER_PAYLOAD_OP_BLEN                 4
#define ENCRYPTED_SERIAL_NUMBER_PAYLOAD_BLEN          141

#define APPLE_SERVER_ENCRYPTION_KEY_BLEN              65
#define APPLE_SERVER_SIG_VERIFICATION_KEY_BLEN        65

#define FINDMY_UUID_SERVICE                           0xFD44
#define UARP_UUID_SERVICE                             0xFD43





#endif /* fmna_constants_h */
