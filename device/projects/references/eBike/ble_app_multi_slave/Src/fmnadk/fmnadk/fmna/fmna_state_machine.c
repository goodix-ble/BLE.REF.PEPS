



#include "fmna_util.h"
#include "fmna_gatt.h"
#include "fmna_state_machine.h"
#include "fmna_adv.h"
#include "fmna_connection.h"
#include "fmna_crypto.h"
#include "fmna_config_control_point.h"


#include "fmna_motion_detection.h"
#include "fmna_nonowner_control_point.h"
#include "fmna_nfc.h"
#include "fmna_paired_owner_control_point.h"


#define MANUF_PAIR_EN_REPORT_TIMEOUT                    600000 /*10 Minutes*/

typedef uint32_t (*fmna_evt_fptr_t) (FMNA_SM_Event_t fmna_evt, void * p_context);

typedef struct
{
    FMNA_SM_Event_t       fmna_evt;             // FIND MY Event
    FMNA_SM_State_t       fmna_state_success;   // New state (Success)
    FMNA_SM_State_t       fmna_state_fail;      // New state (Failure)
    fmna_evt_fptr_t       handler;              // Event Handler
} fmna_evt_handler_t;

typedef struct
{
    fmna_evt_handler_t const *p_array_handlers;
    uint8_t                   count;
} fmna_array_evt_handler_t;

typedef enum
{
    IS_NEARBY_UNINIT,
    IS_NEARBY_FALSE,
    IS_NEARBY_TRUE,
} is_nearby_t;

fmna_primary_key_t m_fmna_current_primary_key = {0};
fmna_primary_key_t m_fmna_current_separated_primary_key = {0};
fmna_secondary_key_t m_fmna_current_secondary_key = {0};
uint32_t m_current_separated_primary_key_index;

uint8_t g_pair_adv_force_stop = 0;
uint16_t g_has_been_maintenanced_conn_idx = FMNA_BLE_CONN_HANDLE_INVALID;

// Log decode strings
const char* fmna_sm_state_strings[] =
{
    "FMNA_BOOT",
    "FMNA_BLE PAIR",
    "FMNA_SEPARATED",
    "FMNA_NEARBY",
    "FMNA_CONNECTING ",
    "FMNA_PAIR",
    "FMNA_PAIRED",
    "FMNA_CONNECTED",
    "FMNA_DISCONNECT",
    "FMNA_NOCHANGE",
};

// Log decode strings
const char* fmna_sm_event_strings[] =
{
    "FMNA_EVENT_BOOT",
    "FMNA_EVENT_NEARBY_SEPARATED_TIMEOUT",
    "FMNA_EVENT_KEY_ROTATE",
    "FMNA_EVENT_BONDED",
    "FMNA_EVENT_UNBONDED",
    "FMNA_EVENT_CONNECTED",
    "FMNA_EVENT_DISCONNECTED",
    "FMNA_EVENT_NEARBY",
    "FMNA_EVENT_SEPARATED",
    "FMNA_EVENT_PAIR",
    "FMNA_EVENT_SOUND_START",
    "FMNA_EVENT_SOUND_STOP",
    "FMNA_EVENT_SOUND_COMPLETE",
    "FMNA_EVENT_LOST_UT_SPEAKER_START",
    "FMNA_EVENT_FMNA_PAIRING_INITIATE",
    "FMNA_EVENT_FMNA_PAIRING_FINALIZE",
    "FMNA_EVENT_FMNA_PAIRING_COMPLETE",
    "FMNA_EVENT_FMNA_PAIRING_MFITOKEN",
    "FMNA_EVENT_MOTION_DETECTED",
#if FMNA_DEBUG_ENABLE
    "FMNA_EVENT_DEBUG_RESET_INTO_SEPARATED",
#endif // DEBUG
};

#define FMNA_EVT_LIST_LENGTH(fmna_evt_handler)      (sizeof(fmna_evt_handler)/sizeof(fmna_evt_handler[0]))

// MARK: Static Functions
// Naming follows:  fmna_<state>_evt_<evt type>_handler
static uint32_t fmna_boot_evt_boot_handler                                           (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_pair_evt_bonded_handler                                         (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_pair_evt_connected_handler                                      (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_pair_evt_disconnected_handler                                   (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_pair_evt_pair_handler                                           (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_separated_evt_connected_handler                                 (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_separated_evt_key_rotate_handler                                (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_separated_evt_unbonded_handler                                  (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_separated_evt_motion_detected_handler                           (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_separated_evt_sound_start_handler                               (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_separated_evt_sound_complete_handler                            (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_nearby_evt_timeout_handler                                      (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_nearby_evt_connected_handler                                    (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_nearby_evt_key_rotate_handler                                   (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_unpaired_connecting_evt_fmna_pairing_initiate_handler           (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_connected_evt_unbonded_handler                                  (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_connected_evt_disconnected_handler                              (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_connected_evt_timeout_handler                                   (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_connected_evt_key_rotate_handler                                (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_connected_evt_sound_stop_handler                                (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_fmna_pair_evt_fmna_pairing_finalize_handler                     (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_fmna_pair_evt_fmna_pairing_mfitoken_handler                     (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_fmna_pair_evt_disconnected_handler                              (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_fmna_pair_complete_evt_fmna_pairing_complete_handler            (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_disconnecting_evt_nearby_handler                                (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_disconnecting_evt_separated_handler                             (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_disconnecting_evt_pair_handler                                  (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_generic_evt_bonded_handler                                      (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_generic_evt_disconnected_handler                                (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_generic_evt_sound_start_handler                                 (FMNA_SM_Event_t fmna_evt, void * p_context);
static uint32_t fmna_generic_evt_sound_complete_handler                              (FMNA_SM_Event_t fmna_evt, void * p_context);
#if FMNA_DEBUG_ENABLE
static uint32_t fmna_connected_evt_debug_reset_handler                               (FMNA_SM_Event_t fmna_evt, void * p_context);
#endif // DEBUG

static void fmna_nearby_separated_timeout_handler(void);
static void fmna_key_rotation_handler(void);
static void fmna_one_time_key_rotation_handler(void);
static void fmna_non_owner_connection_timeout_handler(void);
static void fmna_pair_connection_timeout_handler(void);
static void fmna_persistent_connection_disconnection_timeout_handler(void);
static void fmna_separated_ut_timeout_handler(void);
static void fmna_update_secondary_index(uint32_t current_index);
static void stop_pair_connection_timer(void);

// MARK: - Static Variables

static volatile FMNA_SM_State_t m_fmna_state;

// Persistent connection flag to determine disconnection advertising behavior.
static bool is_persistent_connection_disconnection = false;
static bool m_motion_detected_sound = false;
static is_nearby_t m_is_nearby = IS_NEARBY_UNINIT;
// Flag tracking maintenanced status of accessory.
static bool has_been_maintenanced = false;

static uint32_t m_fmna_nearby_separated_timeout = SEC_TO_MSEC(30);
static fmna_timer_handle_t  m_fmna_nearby_separated_timeout_timer_id;  /**< Nearby to Separated state timeout timer id. */

uint32_t m_fmna_key_rotation_timeout_ms = MIN_TO_MSEC(15);      /**< don't change it,unless is updated the app and server at the same time. */
static fmna_timer_handle_t  m_fmna_key_rotation_timer_id;            /**< Rotation timer id. */


static fmna_timer_handle_t  m_fmna_one_time_key_rotation_timer_id;   /**< One time rotation timer id. */

uint32_t m_fmna_non_owner_connection_timeout = SEC_TO_MSEC(10); // 10 second
static fmna_timer_handle_t  m_fmna_non_owner_connection_timeout_timer_id;  /**< Non-owner connection timeout timer id. */

static uint32_t m_fmna_pair_connection_timeout = SEC_TO_MSEC(10);
static fmna_timer_handle_t  m_fmna_pair_connection_timeout_timer_id;  /**< pair connection timeout timer id. */

static uint32_t m_separated_ut_timeout_ms = DAYS_TO_MSEC(3);    // Default smoke alarm timeout @ 3 days.

static fmna_timer_handle_t  m_separated_ut_timeout_timer_id;  /**< Wild smoke alarm timeout timer id. */
static fmna_timer_handle_t  m_fmna_persistent_connection_disconnection_timer_id;  /**< Rotation timer id. */

static uint32_t cached_next_secondary_key_rotation_index;
static uint32_t next_secondary_key_rotation_index;

fmna_secondary_keys_info_t m_fmna_secondary_keys_info;

// MARK: - Event Handler List
static const fmna_evt_handler_t fmna_sm_boot_evt_handlers[] = \
{
    /* FIND_MY Event                           AppStateSuccess                    AppStateFailure            Handler  */

    { FMNA_SM_EVENT_BOOT,                      FMNA_SM_SEPARATED,                 FMNA_SM_PAIR,              fmna_boot_evt_boot_handler },
};


static const fmna_evt_handler_t fmna_sm_pair_evt_handlers[] = \
{
    /* FIND_MY Event                           AppStateSuccess                    AppStateFailure            Handler */

    { FMNA_SM_EVENT_BONDED,                    FMNA_SM_CONNECTING,                FMNA_SM_CONNECTED,         fmna_pair_evt_bonded_handler },
    { FMNA_SM_EVENT_CONNECTED,                 FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_pair_evt_connected_handler },
    { FMNA_SM_EVENT_DISCONNECTED,              FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_pair_evt_disconnected_handler },
    { FMNA_SM_EVENT_PAIR,                      FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_pair_evt_pair_handler },
};

static const fmna_evt_handler_t fmna_sm_separated_evt_handlers[] = \
{
    /* FIND_MY Event                           AppStateSuccess                    AppStateFailure            Handler */

    { FMNA_SM_EVENT_KEY_ROTATE,                FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_separated_evt_key_rotate_handler },
    { FMNA_SM_EVENT_CONNECTED,                 FMNA_SM_CONNECTED,                 FMNA_SM_NOCHANGE,          fmna_separated_evt_connected_handler },
    { FMNA_SM_EVENT_UNBONDED,                  FMNA_SM_PAIR,                      FMNA_SM_PAIR,              fmna_separated_evt_unbonded_handler },
    { FMNA_SM_EVENT_SOUND_COMPLETE,            FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_separated_evt_sound_complete_handler },
    { FMNA_SM_EVENT_MOTION_DETECTED,           FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_separated_evt_motion_detected_handler },
    { FMNA_SM_EVENT_SOUND_START,               FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_separated_evt_sound_start_handler },
};

static const fmna_evt_handler_t fmna_sm_nearby_evt_handlers[] = \
{
    /* FIND_MY Event                           AppStateSuccess                    AppStateFailure            Handler   */

    { FMNA_SM_EVENT_NEARBY_SEPARATED_TIMEOUT,  FMNA_SM_SEPARATED,                 FMNA_SM_SEPARATED,         fmna_nearby_evt_timeout_handler },
    { FMNA_SM_EVENT_KEY_ROTATE,                FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_nearby_evt_key_rotate_handler },
    { FMNA_SM_EVENT_CONNECTED,                 FMNA_SM_CONNECTED,                 FMNA_SM_NOCHANGE,          fmna_nearby_evt_connected_handler },
    { FMNA_SM_EVENT_SOUND_COMPLETE,            FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_generic_evt_sound_complete_handler },
};

static const fmna_evt_handler_t fmna_sm_connecting_evt_handlers[] = \
{
    /* FIND_MY Event                           AppStateSuccess                    AppStateFailure            Handler   */

    { FMNA_SM_EVENT_DISCONNECTED,              FMNA_SM_DISCONNECTING,             FMNA_SM_NOCHANGE,          fmna_fmna_pair_evt_disconnected_handler },
    { FMNA_SM_EVENT_FMNA_PAIRING_INITIATE,     FMNA_SM_FMNA_PAIR,                 FMNA_SM_NOCHANGE,          fmna_unpaired_connecting_evt_fmna_pairing_initiate_handler },
};

static const fmna_evt_handler_t fmna_sm_fmna_pair_evt_handlers[] = \
{
    /* FIND_MY Event                           AppStateSuccess                    AppStateFailure            Handler  */

    { FMNA_SM_EVENT_FMNA_PAIRING_FINALIZE,     FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_fmna_pair_evt_fmna_pairing_finalize_handler },
    { FMNA_SM_EVENT_FMNA_PAIRING_MFITOKEN,     FMNA_SM_FMNA_PAIR_COMPLETE,        FMNA_SM_NOCHANGE,          fmna_fmna_pair_evt_fmna_pairing_mfitoken_handler },
    { FMNA_SM_EVENT_DISCONNECTED,              FMNA_SM_DISCONNECTING,             FMNA_SM_NOCHANGE,          fmna_fmna_pair_evt_disconnected_handler},

};

static const fmna_evt_handler_t fmna_sm_fmna_pair_complete_evt_handlers[] = \
{
    /* FIND_MY Event                           AppStateSuccess                    AppStateFailure            Handler  */

    { FMNA_SM_EVENT_FMNA_PAIRING_COMPLETE,     FMNA_SM_CONNECTED,                 FMNA_SM_NOCHANGE,          fmna_fmna_pair_complete_evt_fmna_pairing_complete_handler },
    { FMNA_SM_EVENT_DISCONNECTED,              FMNA_SM_DISCONNECTING,             FMNA_SM_NOCHANGE,          fmna_fmna_pair_evt_disconnected_handler},

};

static const fmna_evt_handler_t fmna_sm_connected_evt_handlers[] = \
{
    /* FIND_MY Event                           AppStateSuccess                    AppStateFailure            Handler   */
    { FMNA_SM_EVENT_BONDED,                    FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_generic_evt_bonded_handler },
    { FMNA_SM_EVENT_UNBONDED,                  FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_connected_evt_unbonded_handler },
    { FMNA_SM_EVENT_DISCONNECTED,              FMNA_SM_DISCONNECTING,             FMNA_SM_NOCHANGE,          fmna_connected_evt_disconnected_handler },
    { FMNA_SM_EVENT_KEY_ROTATE,                FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_connected_evt_key_rotate_handler },
    { FMNA_SM_EVENT_NEARBY_SEPARATED_TIMEOUT,  FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_connected_evt_timeout_handler },
    { FMNA_SM_EVENT_SOUND_START,               FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_generic_evt_sound_start_handler },
    { FMNA_SM_EVENT_SOUND_STOP,                FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_connected_evt_sound_stop_handler },
    { FMNA_SM_EVENT_SOUND_COMPLETE,            FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_generic_evt_sound_complete_handler },
#if FMNA_DEBUG_ENABLE
    { FMNA_SM_EVENT_DEBUG_RESET_INTO_SEPARATED,FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_connected_evt_debug_reset_handler },
#endif // DEBUG
};

static const fmna_evt_handler_t fmna_sm_disconnecting_evt_handlers[] = \
{
    /* FIND_MY Event                           AppStateSuccess                    AppStateFailure            Handler  */

    { FMNA_SM_EVENT_NEARBY,                    FMNA_SM_NEARBY,                    FMNA_SM_NEARBY,            fmna_disconnecting_evt_nearby_handler },
    { FMNA_SM_EVENT_SEPARATED,                 FMNA_SM_SEPARATED,                 FMNA_SM_SEPARATED,         fmna_disconnecting_evt_separated_handler },
    { FMNA_SM_EVENT_PAIR,                      FMNA_SM_PAIR,                      FMNA_SM_PAIR,              fmna_disconnecting_evt_pair_handler },
    { FMNA_SM_EVENT_SOUND_COMPLETE,            FMNA_SM_NOCHANGE,                  FMNA_SM_NOCHANGE,          fmna_generic_evt_sound_complete_handler },

};

static const fmna_array_evt_handler_t fmna_sm_handlers[] = \
{
    {fmna_sm_boot_evt_handlers,                      FMNA_EVT_LIST_LENGTH(fmna_sm_boot_evt_handlers)},
    {fmna_sm_pair_evt_handlers,                      FMNA_EVT_LIST_LENGTH(fmna_sm_pair_evt_handlers)},
    {fmna_sm_separated_evt_handlers,                 FMNA_EVT_LIST_LENGTH(fmna_sm_separated_evt_handlers)},
    {fmna_sm_nearby_evt_handlers,                    FMNA_EVT_LIST_LENGTH(fmna_sm_nearby_evt_handlers)},
    {fmna_sm_connecting_evt_handlers,                FMNA_EVT_LIST_LENGTH(fmna_sm_connecting_evt_handlers)},
    {fmna_sm_fmna_pair_evt_handlers,                 FMNA_EVT_LIST_LENGTH(fmna_sm_fmna_pair_evt_handlers)},
    {fmna_sm_fmna_pair_complete_evt_handlers,        FMNA_EVT_LIST_LENGTH(fmna_sm_fmna_pair_complete_evt_handlers)},
    {fmna_sm_connected_evt_handlers,                 FMNA_EVT_LIST_LENGTH(fmna_sm_connected_evt_handlers)},
    {fmna_sm_disconnecting_evt_handlers,             FMNA_EVT_LIST_LENGTH(fmna_sm_disconnecting_evt_handlers)},
};

#define FMNA_EVT_LIST_LEN(evt_handlers) (sizeof(evt_handlers)/sizeof(evt_handlers[0]))



static void start_pair_adv(void)
{
    fmna_adv_init_pairing();
    fmna_adv_start_fast_adv();
}

static void set_is_nearby(is_nearby_t new_is_nearby)
{
    switch (new_is_nearby)
    {
        case IS_NEARBY_TRUE:
            if (m_is_nearby != IS_NEARBY_TRUE)
            {
                // Turn off motion detection if we're going into Nearby.
                fmna_motion_detection_stop();

                // If we're transitioning to Nearby, turn the Separated UT motion detection timer off.
                FMNA_LOG_INFO("Stop Separated UT motion detection timer");
                fmna_timer_stop(&m_separated_ut_timeout_timer_id);
            }
            break;

        case IS_NEARBY_FALSE:
            if (m_is_nearby != IS_NEARBY_FALSE)
            {
                // If we are transitioning out of Nearby, start the Separated UT motion detection timer.
                FMNA_LOG_INFO("Start Separated UT motion detection timer (%d ms)", m_separated_ut_timeout_ms);
                bool ret = fmna_timer_start(&m_separated_ut_timeout_timer_id, m_separated_ut_timeout_ms);
                FMNA_BOOL_CHECK(ret);
            }
            break;

        case IS_NEARBY_UNINIT:
            // This should never happen.
            FMNA_ERROR_CHECK(FMNA_ERROR_INVALID_STATE);
            break;

        default:
            FMNA_ERROR_CHECK(FMNA_ERROR_INVALID_STATE);
            break;
    }

    m_is_nearby = new_is_nearby;
}

static void stop_pair_connection_timer(void)
{
    // Stop pair timeout timer
    FMNA_LOG_DEBUG("Stop fmna pair connection timer");
    fmna_timer_stop(&m_fmna_pair_connection_timeout_timer_id);
}

/// Boots into to Pairing or Separated based on paired status.
static uint32_t fmna_boot_evt_boot_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    // Check if we are paired or not. If we are NOT paired, force unpair (for sanity cleanup) and go into the pairing state, start advertising.
    if(fmna_connection_is_fmna_paired() == false)
    {
        // Unpair just in case, ensure that we are really unpaired before booting into Pairing state.
        fmna_pm_delete_bonds();

        fmna_adv_reset_bd_addr();

        #if FMNA_NFC_ENABLE
        fmna_nfc_load_unpaired_url();
        #endif

        // Go into Pairing state
        start_pair_adv();

        return FMNA_SM_STATUS_NOT_BONDED;
    }

    // If app is paired, proceed
    FMNA_LOG_INFO("Start key rotation (%d ms)", m_fmna_key_rotation_timeout_ms);
    bool ret = fmna_timer_start(&m_fmna_key_rotation_timer_id, m_fmna_key_rotation_timeout_ms);
    FMNA_BOOL_CHECK(ret);

    //TODO: Read/get/generate current primary key & current secondary key.

    // since this is a boot we need to see if the recovery index has moved on
    fmna_update_secondary_index(m_fmna_current_primary_key.index);

    FMNA_LOG_DEBUG("Current secondary key preview:");
    FMNA_LOG_HEXDUMP_DEBUG(m_fmna_current_secondary_key.public_key, 4);
    FMNA_LOG_DEBUG("Current primary key preview:");
    FMNA_LOG_HEXDUMP_DEBUG(m_fmna_current_primary_key.public_key, 4);

    fmna_connection_set_active_ltk(m_fmna_current_primary_key.ltk);

    #if FMNA_NFC_ENABLE
    fmna_nfc_load_paired_url();
    #endif

    // Go into separated state
    fmna_adv_init_separated(m_fmna_current_secondary_key.public_key, m_fmna_current_primary_key.public_key[FMNA_SEPARATED_ADV_PUBKEY_HINT_INDEX]);
    fmna_adv_start_slow_adv();

    set_is_nearby(IS_NEARBY_FALSE);

    return FMNA_SM_STATUS_SUCCESS;
}

/// Pairing to Connected event handler.
static uint32_t fmna_pair_evt_bonded_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    return FMNA_SM_STATUS_SUCCESS;
}

/// Starts pairing connection timer.
static uint32_t fmna_pair_evt_connected_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    FMNA_LOG_INFO("Start fmna pair connection timer (%d ms)", m_fmna_pair_connection_timeout);
    bool ret_code = fmna_timer_start(&m_fmna_pair_connection_timeout_timer_id, m_fmna_pair_connection_timeout);
    if (!ret_code)
    {
        FMNA_LOG_WARNING("Start fmna pair connection timer err %d, so force disconnect", ret_code);
        // Force disconnect if this timer fails to start.
        fmna_connection_disconnect_this();
        return FMNA_SM_STATUS_FAIL;
    }

    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_pair_evt_disconnected_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    stop_pair_connection_timer();

    start_pair_adv();

    return FMNA_SM_STATUS_NOT_BONDED;
}

static uint32_t fmna_pair_evt_pair_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    start_pair_adv();
    return FMNA_SM_STATUS_SUCCESS;
}

/// Re-initializes Separated advertisement with the next keys.
static uint32_t fmna_separated_evt_key_rotate_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    // Check what key we should be beaconing in the Separated state with.
    if (memcmp_val(m_fmna_current_separated_primary_key.public_key, 0, FMNA_PUBKEY_BLEN))
    {
        FMNA_LOG_HEXDUMP_DEBUG(m_fmna_current_secondary_key.public_key, 4);
        fmna_adv_init_separated(m_fmna_current_secondary_key.public_key, m_fmna_current_primary_key.public_key[FMNA_SEPARATED_ADV_PUBKEY_HINT_INDEX]);
    }
    else
    {
        fmna_adv_init_separated(m_fmna_current_separated_primary_key.public_key, m_fmna_current_primary_key.public_key[FMNA_SEPARATED_ADV_PUBKEY_HINT_INDEX]);
    }
    fmna_adv_start_slow_adv();

    return FMNA_SM_STATUS_SUCCESS;
}

/// Starts non owner connection timeout. Fires and disconnects if link is not encrypted in time.
static uint32_t fmna_separated_evt_connected_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    bool ret_code = fmna_timer_start(&m_fmna_non_owner_connection_timeout_timer_id, m_fmna_non_owner_connection_timeout);
    if (!ret_code)
    {
        // Force disconnect if this timer fails to start.
        fmna_connection_disconnect_this();
        return FMNA_SM_STATUS_FAIL;
    }

    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_separated_evt_unbonded_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    start_pair_adv();
    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_separated_evt_sound_complete_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    if (m_motion_detected_sound)
    {
        // Sound complete from motion detected play sound in Separated state.
        m_motion_detected_sound = false;

        // Detected motion, so (re)start active polling.
        fmna_motion_detection_start_active_polling();

        return FMNA_SM_STATUS_SUCCESS;
    }
    else
    {
        // Regular play sound, connection initiated.
        return fmna_generic_evt_sound_complete_handler(fmna_evt, p_context);
    }
}

static uint32_t fmna_separated_evt_motion_detected_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    // Motion detected in Separated state; play sound.

    fmna_state_machine_dispatch_event(FMNA_SM_EVENT_SOUND_START);

    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_separated_evt_sound_start_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    if (m_motion_detected_sound)
    {
        FMNA_LOG_INFO("Motion detected sound is playing");
        return FMNA_SM_STATUS_SUCCESS;
    }
    m_motion_detected_sound = true;

    return fmna_generic_evt_sound_start_handler(fmna_evt, p_context);
}

/// Reinitializes advertismenets and switches to Separated advertising, with relevant keys.
static uint32_t fmna_nearby_evt_timeout_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    // Begin separated advertisement
    if (memcmp_val(m_fmna_current_separated_primary_key.public_key, 0, FMNA_PUBKEY_BLEN))
    {
        FMNA_LOG_DEBUG("Current Secondary Key:");
        FMNA_LOG_HEXDUMP_DEBUG(m_fmna_current_secondary_key.public_key, 4);
        fmna_adv_init_separated(m_fmna_current_secondary_key.public_key, m_fmna_current_primary_key.public_key[FMNA_SEPARATED_ADV_PUBKEY_HINT_INDEX]);
    }
    else
    {
        fmna_adv_init_separated(m_fmna_current_separated_primary_key.public_key, m_fmna_current_primary_key.public_key[FMNA_SEPARATED_ADV_PUBKEY_HINT_INDEX]);
    }

    fmna_adv_start_slow_adv();

    set_is_nearby(IS_NEARBY_FALSE);

    return FMNA_SM_STATUS_SUCCESS;
}

/// Re-initializes Nearby advertisement with new key.
static uint32_t fmna_nearby_evt_key_rotate_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    fmna_adv_init_nearby(m_fmna_current_primary_key.public_key);
    fmna_adv_start_slow_adv();
    return FMNA_SM_STATUS_SUCCESS;
}

/// Starts non owner connection timer. Fires and disconnects if link is not encrypted in time.
static uint32_t fmna_nearby_evt_connected_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    bool ret_code = fmna_timer_start(&m_fmna_non_owner_connection_timeout_timer_id, m_fmna_non_owner_connection_timeout);
    if (!ret_code)
    {
        // Force disconnect if this timer fails to start.
        fmna_connection_disconnect_this();
        return FMNA_SM_STATUS_FAIL;
    }

    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_generic_evt_sound_complete_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    uint16_t sound_conn_handle = fmna_connection_get_conn_handle_with_multi_status_enabled(FMNA_MULTI_STATUS_PLAYING_SOUND);

    if (sound_conn_handle == FMNA_BLE_CONN_HANDLE_INVALID)
    {
        FMNA_LOG_INFO("Sound initiator no longer connected");
        return FMNA_SM_STATUS_SUCCESS;
    }

    if (fmna_connection_is_status_bit_enabled(sound_conn_handle, FMNA_MULTI_STATUS_ENCRYPTED))
    {
        fmna_gatt_send_indication(sound_conn_handle, FMNA_SERVICE_OPCODE_SOUND_COMPLETED, NULL, 0);
    }
    else
    {
        fmna_gatt_send_indication(sound_conn_handle, FMNA_SERVICE_NON_OWNER_OPCODE_SOUND_COMPLETED, NULL, 0);
    }

    fmna_connection_update_connection_info_all(FMNA_MULTI_STATUS_PLAYING_SOUND, false);

    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_fmna_pair_evt_disconnected_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    // Pairing variables/buffer cleanup
    return fmna_generic_evt_disconnected_handler(fmna_evt, p_context);
}

static uint32_t fmna_unpaired_connecting_evt_fmna_pairing_initiate_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    stop_pair_connection_timer();

    fmna_ret_code_t ret_code = FMNA_SUCCESS;
    uint32_t sm_ret = FMNA_SM_STATUS_SUCCESS;

    ret_code = fmna_crypto_generate_send_pairing_data_params();

    if (FMNA_SUCCESS != ret_code)
    {
        // Failure to generate send_pairing_data params, disconnect and end this pairing attempt.
        FMNA_LOG_WARNING("Failure to generate send_pairing_data params, disconnect and end this pairing attempt");
        fmna_connection_disconnect_this();
        sm_ret = FMNA_SM_STATUS_CRYPTO_FAIL;
    }
    else
    {
        // Successfully generated send_pairing_data params, send response to central, and go into App Pairing state.
        fmna_gatt_send_indication(fmna_gatt_get_most_recent_conn_handle(), FMNA_SERVICE_OPCODE_SEND_PAIRING_DATA, &m_fmna_send_pairing_data, sizeof(m_fmna_send_pairing_data));
    }

    return sm_ret;
}

//MARK: Find My Pair State Event Handlers
static uint32_t fmna_fmna_pair_evt_fmna_pairing_finalize_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    fmna_ret_code_t ret_code = FMNA_SUCCESS;
    uint32_t sm_ret = FMNA_SM_STATUS_SUCCESS;

    ret_code = fmna_crypto_finalize_pairing();

    if (FMNA_SUCCESS != ret_code)
    {
        FMNA_LOG_WARNING("Failure to finalize pairing, disconnect and end this pairing attempt");
        fmna_connection_disconnect_this();
        sm_ret = FMNA_SM_STATUS_CRYPTO_FAIL;
    }

    return sm_ret;
}

static uint32_t fmna_fmna_pair_evt_fmna_pairing_mfitoken_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    uint32_t sm_ret = FMNA_SM_STATUS_SUCCESS;
    bool token_stored = fmna_connection_mfi_token_stored();

    if (token_stored == false)
    {
        FMNA_LOG_WARNING("Software auth token stored fail, so disconnected");
        // Failure to finalize pairing - store the MFi token, disconnect and end this pairing attempt.
        fmna_connection_disconnect_this();
        sm_ret = FMNA_SM_STATUS_CRYPTO_FAIL;
    }
    else
    {
        // Successfully finalized pairing, send response to central, and go into Connected state.
        FMNA_LOG_WARNING("Accessory Send pairing status");
        fmna_gatt_send_indication(fmna_gatt_get_most_recent_conn_handle(), FMNA_SERVICE_OPCODE_SEND_PAIRING_STATUS, &m_fmna_send_pairing_status, sizeof(m_fmna_send_pairing_status));
    }

    return sm_ret;
}

static uint32_t fmna_fmna_pair_complete_evt_fmna_pairing_complete_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    fmna_connection_set_is_fmna_paired(true);

    fmna_sound_platform_paired();

    fmna_crypto_pairing_complete();

    set_is_nearby(IS_NEARBY_TRUE);

    // SK_Primary_N -> SK_Primary_0
    fmna_ret_code_t ret_code = fmna_crypto_roll_primary_sk();
    FMNA_ERROR_CHECK(ret_code);

    // SK_Secondary_N -> SK_Secondary_0
    ret_code = fmna_crypto_roll_secondary_sk();
    FMNA_ERROR_CHECK(ret_code);

    // Roll to P_Primary_1
    ret_code = fmna_crypto_roll_primary_key();
    FMNA_ERROR_CHECK(ret_code);

    // Roll to P_Secondary_1
    ret_code = fmna_crypto_roll_secondary_key();
    FMNA_ERROR_CHECK(ret_code);

    FMNA_LOG_INFO("Start key rotation (%d ms)", m_fmna_key_rotation_timeout_ms);
    bool ret = fmna_timer_start(&m_fmna_key_rotation_timer_id, m_fmna_key_rotation_timeout_ms);
    FMNA_BOOL_CHECK(ret);

    #if FMNA_NFC_ENABLE
    fmna_nfc_load_paired_url();
    #endif

    return FMNA_SM_STATUS_SUCCESS;
}

/// Handler for Owner connecting and encrypting the link successfully.
/// @details     Disable Nearby->Separated timer, and non-owner connection timer.
///              In multi-scenario, if there is still an unencrypted connection, restart the
///              non-owner connection timeout for that connection.
static uint32_t fmna_generic_evt_bonded_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    uint8_t periph_link_cnt = fmna_connection_get_num_connections();
    set_is_nearby(IS_NEARBY_TRUE);
    
    // Set the maintenanced flag to be true since we just connected to an owner.
    has_been_maintenanced = true;
    g_has_been_maintenanced_conn_idx = fmna_gatt_platform_get_most_recent_conn_handle();
    // only stop the Nearby -> Separated timeout timer if the owner connects from Nearby and encrypts the link
    fmna_timer_stop(&m_fmna_nearby_separated_timeout_timer_id);

    // turn off the non-owner timeout disconnect timer
    fmna_timer_stop(&m_fmna_non_owner_connection_timeout_timer_id);

    uint32_t new_timer = fmna_connection_get_non_owner_timeout();
    if (new_timer)
    {
        bool ret = fmna_timer_start(&m_fmna_non_owner_connection_timeout_timer_id, new_timer);
        FMNA_BOOL_CHECK(ret);
    }

    if (periph_link_cnt < fmna_connection_get_max_connections())
    {
        fmna_adv_init_nearby(m_fmna_current_primary_key.public_key);

        if (is_persistent_connection_disconnection)
        {
            fmna_adv_start_fast_adv();
        }
        else
        {
            fmna_adv_start_slow_adv();
        }
    }

    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_connected_evt_unbonded_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_connected_evt_disconnected_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    // If another Owner is still connected, we want to clean up the disconnecting device but still stay in the Connected state.
    if (fmna_connection_is_status_bit_enabled(FMNA_BLE_CONN_HANDLE_INVALID, FMNA_MULTI_STATUS_ENCRYPTED))
    {
        return FMNA_SM_STATUS_FAIL;
    }

    return fmna_generic_evt_disconnected_handler(fmna_evt, p_context);
}

/// Notify iOS of keyroll, and re-initialize Nearby ADV with new key, if applicable.
static uint32_t fmna_connected_evt_key_rotate_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    // Broadcast this message to all valid centrals.
    for (uint16_t conn_handle = 0; conn_handle < FMNA_BLE_MAX_SUPPORTED_CONNECTIONS; conn_handle++)
    {
        if (FMNA_BLE_CONN_HANDLE_INVALID != m_fmna_active_connections[conn_handle].conn_handle) {
            // Send indication to the specific central.
            fmna_gatt_send_indication(conn_handle, FMNA_SERVICE_OPCODE_KEYROLL_INDICATION, &(m_fmna_current_primary_key.index), sizeof(m_fmna_current_primary_key.index));
        }
    }

    // rotate adv for multiple connections
    if (fmna_connection_get_num_connections() && fmna_connection_get_num_connections() < fmna_connection_get_max_connections())
    {
        fmna_adv_init_nearby(m_fmna_current_primary_key.public_key);
        fmna_adv_start_slow_adv();
    }

    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_connected_evt_timeout_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    // link might not be encrypted yet and Nearby to Separated timeout fired
    // the next time we disconnect, we should disconnect into Separated.

    for (uint16_t conn_handle = 0; conn_handle < FMNA_BLE_MAX_SUPPORTED_CONNECTIONS; ++conn_handle)
    {
        if (m_fmna_active_connections[conn_handle].conn_handle != FMNA_BLE_CONN_HANDLE_INVALID)
        {
            if (fmna_state_machine_is_nearby() | fmna_connection_is_status_bit_enabled(conn_handle, FMNA_MULTI_STATUS_ENCRYPTED))
            {
                set_is_nearby(IS_NEARBY_TRUE);
            }
            else
            {
                set_is_nearby(IS_NEARBY_FALSE);
            }
        }
     }

    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_generic_evt_sound_start_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    fmna_sound_platform_found_start();
    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_connected_evt_sound_stop_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    fmna_sound_platform_found_stop();
    return FMNA_SM_STATUS_SUCCESS;
}

//MARK: Disconnecting State Event Handlers
static uint32_t fmna_disconnecting_evt_nearby_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    fmna_adv_init_nearby(m_fmna_current_primary_key.public_key);

    if (is_persistent_connection_disconnection)
    {
        fmna_adv_start_fast_adv();
        is_persistent_connection_disconnection = false;
    }
    else
    {
        fmna_adv_start_slow_adv();
    }
    fmna_timer_stop(&m_fmna_persistent_connection_disconnection_timer_id);//fix the nearby fast adv less than 3 seconds
    fmna_timer_stop(&m_fmna_nearby_separated_timeout_timer_id);
    bool ret = fmna_timer_start(&m_fmna_nearby_separated_timeout_timer_id, m_fmna_nearby_separated_timeout);
    FMNA_BOOL_CHECK(ret);

    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_disconnecting_evt_separated_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    // begin Separated advertisement
    if (memcmp_val(m_fmna_current_separated_primary_key.public_key, 0, FMNA_PUBKEY_BLEN))
    {
        FMNA_LOG_HEXDUMP_DEBUG(m_fmna_current_secondary_key.public_key, 4);
        fmna_adv_init_separated(m_fmna_current_secondary_key.public_key, m_fmna_current_primary_key.public_key[FMNA_SEPARATED_ADV_PUBKEY_HINT_INDEX]);
    }
    else
    {
        fmna_adv_init_separated(m_fmna_current_separated_primary_key.public_key, m_fmna_current_primary_key.public_key[FMNA_SEPARATED_ADV_PUBKEY_HINT_INDEX]);
    }

    if (m_aggressive_ut_adv_enabled)
    {
        m_aggressive_ut_adv_enabled = false;
        fmna_adv_start_fast_adv();
    }
    else
    {
        fmna_adv_start_slow_adv();
    }

    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_disconnecting_evt_pair_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    start_pair_adv();
    return FMNA_SM_STATUS_SUCCESS;
}

static uint32_t fmna_generic_evt_disconnected_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    bool is_paired = fmna_connection_is_fmna_paired();

    // check to see if this disconnection is to complete an unpair
    if (fmna_connection_get_unpair_pending() == true)
    {
        fmna_connection_set_unpair_pending(false);
        // set is paired flag to false so that we unpair on this disconnect
        is_paired = false;
    }

    // connected to owner
    if (is_paired && fmna_state_machine_is_nearby())
    {
        fmna_state_machine_dispatch_event(FMNA_SM_EVENT_NEARBY);
    }
    // connected to anyone else
    else if (is_paired && !fmna_state_machine_is_nearby())
    {
        fmna_state_machine_dispatch_event(FMNA_SM_EVENT_SEPARATED);
    }
    // unpaired from owner
    else
    {
        // Make sure we are BT unpaired as well.
        if (fmna_pm_peer_count() > 0)
        {
            fmna_connection_fmna_unpair(false);
        }
        fmna_state_machine_dispatch_event(FMNA_SM_EVENT_PAIR);
    }

    // turn off the non-owner timeout disconnect timer
    fmna_timer_stop(&m_fmna_non_owner_connection_timeout_timer_id);

    // if the disconnect is for an already encrypted device
    // restart the guard timer if required
    uint32_t new_timer = fmna_connection_get_non_owner_timeout();
    if (new_timer)
    {
        bool ret = fmna_timer_start(&m_fmna_non_owner_connection_timeout_timer_id, new_timer);
        FMNA_BOOL_CHECK(ret);
    }

    return FMNA_SM_STATUS_SUCCESS;
}

#if FMNA_DEBUG_ENABLE
static uint32_t fmna_connected_evt_debug_reset_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    // Disconnect into Separated mode and start beaconing secondary key.

    set_is_nearby(IS_NEARBY_FALSE);

    // Clear the latched separated key so that we start advertising secondary on disconnect
    memset(m_fmna_current_separated_primary_key.public_key, 0, FMNA_PUBKEY_BLEN);

    // Disconnect to go into Separated.
    fmna_connection_disconnect_all();

    return FMNA_SM_STATUS_SUCCESS;
}
#endif // DEBUG

/// Function for handling the timeout to transition to SEPARATED.
/// @param p_context Pointer used for passing information app_start_timer() was called.
static void fmna_nearby_separated_timeout_handler(void)
{
    fmna_state_machine_dispatch_event(FMNA_SM_EVENT_NEARBY_SEPARATED_TIMEOUT);
}
static void dispatch_update_next_secondary_key_rotation_index(void *p_event_data, uint16_t event_size)
 {
    if (cached_next_secondary_key_rotation_index == m_fmna_secondary_keys_info.next_secondary_key_rotation_index)
     {
        m_fmna_secondary_keys_info.next_secondary_key_rotation_index += PRIMARY_KEYS_PER_SECONDARY_KEY;
    }
}
 
static void fmna_rotate_key(void) {
    cached_next_secondary_key_rotation_index = m_fmna_secondary_keys_info.next_secondary_key_rotation_index;

    fmna_ret_code_t ret_code = fmna_crypto_roll_primary_key();
    FMNA_ERROR_CHECK(ret_code);

    FMNA_LOG_HEXDUMP_DEBUG(m_fmna_current_primary_key.public_key, 4);

    if (m_fmna_current_primary_key.index == cached_next_secondary_key_rotation_index) {
        if (m_fmna_state == FMNA_SM_SEPARATED) {
            m_current_separated_primary_key_index = 0;
            memset(&m_fmna_current_separated_primary_key, 0, sizeof(fmna_primary_key_t));
        }

        fmna_update_secondary_index(m_fmna_current_primary_key.index);

        FMNA_LOG_HEXDUMP_DEBUG(m_fmna_current_secondary_key.public_key, 4);

        fmna_queue_platform_evt_put(NULL, 0, dispatch_update_next_secondary_key_rotation_index);
    }
}

/// Function for handling the timeout to rotate keys.
/// @param p_context Pointer used for passing information app_start_timer() was called.
static void fmna_key_rotation_handler(void)
{
    FMNA_LOG_INFO("Rotating keys...");
    fmna_rotate_key();

    // if we are conencted and encrypted we are maintenanced
    if (fmna_connection_is_status_bit_enabled(FMNA_BLE_CONN_HANDLE_INVALID, FMNA_MULTI_STATUS_ENCRYPTED))
    {
        has_been_maintenanced = true;
    }
    else
    {
        has_been_maintenanced = false;
    }

    fmna_state_machine_dispatch_event(FMNA_SM_EVENT_KEY_ROTATE);
}

static void fmna_one_time_key_rotation_handler(void)
{
    bool ret_code = fmna_timer_start(&m_fmna_key_rotation_timer_id, m_fmna_key_rotation_timeout_ms);
    FMNA_BOOL_CHECK(ret_code);

    fmna_key_rotation_handler();
}

/// Function for handling the non-owner connection timeout.
/// @param p_context Pointer used for passing information app_start_timer() was called.
static void fmna_non_owner_connection_timeout_handler(void)
{
    fmna_connection_disconnect_this();
}

/// Disconnect link if pairing not initiated and this timer fired.
/// @param p_context Pointer used for passing information app_start_timer() was called.
static void fmna_pair_connection_timeout_handler(void)
{
    FMNA_LOG_INFO("FMNA pairing connection timeout, so force disconnect");
    fmna_connection_disconnect_this();
}

static void fmna_separated_ut_timeout_handler(void)
{
    FMNA_LOG_INFO("Enough time has passed that we need to enable motion detection and play sound on movement");
    fmna_motion_detection_start();
}

static void fmna_persistent_connection_disconnection_timeout_handler(void)
{
    is_persistent_connection_disconnection = false;
    if (fmna_connection_get_num_connections() < fmna_connection_get_max_connections())
    {
        fmna_adv_init_nearby(m_fmna_current_primary_key.public_key);
        fmna_adv_start_slow_adv();
    }
}

static void fmna_state_machine_timers_init(void)
{
    bool ret_code;

    ret_code = fmna_timer_create(&m_fmna_nearby_separated_timeout_timer_id, false, fmna_nearby_separated_timeout_handler);
    FMNA_BOOL_CHECK(ret_code);

    ret_code = fmna_timer_create(&m_fmna_key_rotation_timer_id, true, fmna_key_rotation_handler);
    FMNA_BOOL_CHECK(ret_code);

    ret_code = fmna_timer_create(&m_fmna_one_time_key_rotation_timer_id, false, fmna_one_time_key_rotation_handler);
    FMNA_BOOL_CHECK(ret_code);

    ret_code = fmna_timer_create(&m_fmna_non_owner_connection_timeout_timer_id, false, fmna_non_owner_connection_timeout_handler);
    FMNA_BOOL_CHECK(ret_code);

    ret_code = fmna_timer_create(&m_fmna_pair_connection_timeout_timer_id, false, fmna_pair_connection_timeout_handler);
    FMNA_BOOL_CHECK(ret_code);

    ret_code = fmna_timer_create(&m_separated_ut_timeout_timer_id, false, fmna_separated_ut_timeout_handler);
    FMNA_BOOL_CHECK(ret_code);

    ret_code = fmna_timer_create(&m_fmna_persistent_connection_disconnection_timer_id, false, fmna_persistent_connection_disconnection_timeout_handler);
    FMNA_BOOL_CHECK(ret_code);
}

static void dispatch_fmna_sm_event_handler(void *p_event_data, uint16_t event_size)
{
    fmna_evt_handler(*(FMNA_SM_Event_t *)(p_event_data), NULL);
}

void fmna_state_machine_dispatch_event(FMNA_SM_Event_t fmna_evt)
{
    FMNA_LOG_INFO("Push event requeset: %s", fmna_sm_event_strings[fmna_evt]);
    fmna_queue_platform_evt_put(&fmna_evt, sizeof(fmna_evt), dispatch_fmna_sm_event_handler);
}

void fmna_state_machine_init(void)
{
    m_fmna_state = FMNA_SM_BOOT;

    fmna_state_machine_timers_init();
    fmna_state_machine_dispatch_event(FMNA_SM_EVENT_BOOT);
}

void fmna_state_machine_set_nearby_timeout_seconds(uint16_t nearby_timeout_seconds)
{
    m_fmna_nearby_separated_timeout = SEC_TO_MSEC(nearby_timeout_seconds);
}

void fmna_state_machine_set_next_keyroll_ms(uint32_t next_keyroll_ms)
{
    fmna_state_machine_stop_key_rotation_timers();

    FMNA_LOG_DEBUG("Start one time key rotation timer(%d ms)", next_keyroll_ms);
    bool ret_code = fmna_timer_start(&m_fmna_one_time_key_rotation_timer_id, next_keyroll_ms);
    FMNA_BOOL_CHECK(ret_code);
}

void fmna_state_machine_set_separated_ut_timeout_seconds(uint32_t separated_ut_timeout_seconds)
{
    m_separated_ut_timeout_ms = SEC_TO_MSEC(separated_ut_timeout_seconds);
    FMNA_LOG_DEBUG("Setting separated ut timeout to %dms", m_separated_ut_timeout_ms);
}

void fmna_state_machine_set_key_rotation_timeout_ms(uint32_t key_rotation_timeout_ms)
{
    FMNA_LOG_DEBUG("Setting key rotation timeout to %dms", key_rotation_timeout_ms);

    // Set key rotation timeout and restart the timer.
    m_fmna_key_rotation_timeout_ms = key_rotation_timeout_ms;

    fmna_state_machine_stop_key_rotation_timers();

    bool ret_code = fmna_timer_start(&m_fmna_one_time_key_rotation_timer_id, m_fmna_key_rotation_timeout_ms);
    FMNA_BOOL_CHECK(ret_code);
}

uint32_t fmna_state_machine_get_non_owner_connection_timeout(void)
{
    return m_fmna_non_owner_connection_timeout;
}

bool fmna_state_machine_is_persistent_connection_disconnection(void)
{
    return is_persistent_connection_disconnection;
}

bool fmna_state_machine_has_been_maintenanced(void)
{
    return has_been_maintenanced;
}

void fmna_state_machine_maintenanced_clean(uint8_t conn_idx)
{
    has_been_maintenanced = 0;
}

uint16_t fmna_state_machine_maintenanced_connidx_get(void)
{
    return g_has_been_maintenanced_conn_idx;
}

void fmna_state_machine_set_persistent_connection_disconnection(bool persistent_connection_disconnection)
{
    bool ret_code;
    if (persistent_connection_disconnection)
    {
        ret_code = fmna_timer_start(&m_fmna_persistent_connection_disconnection_timer_id, fmna_nearby_adv_fast_duration);
        FMNA_BOOL_CHECK(ret_code);
    }
    else
    {
        fmna_timer_stop(&m_fmna_persistent_connection_disconnection_timer_id);
    }
    is_persistent_connection_disconnection = persistent_connection_disconnection;
}

void fmna_state_machine_stop_key_rotation_timers(void)
{
    fmna_timer_stop(&m_fmna_key_rotation_timer_id);
    fmna_timer_stop(&m_fmna_one_time_key_rotation_timer_id);
}


static void dispatch_set_next_secondary_key_rotation_index_handler(void *p_event_data, uint16_t event_size)
{
    FMNA_LOG_DEBUG("next recovery key rotation index = %d", next_secondary_key_rotation_index);
    uint32_t current_index = m_fmna_current_primary_key.index;

    if (next_secondary_key_rotation_index > current_index)
    {
        m_fmna_secondary_keys_info.next_secondary_key_rotation_index = next_secondary_key_rotation_index;
    }
    else
    {
        //latch current separated key *and* re-align the next recovery key rotation index
        memcpy(&m_fmna_current_separated_primary_key, &m_fmna_current_primary_key, sizeof(fmna_primary_key_t));
        m_current_separated_primary_key_index = current_index;
        m_fmna_secondary_keys_info.next_secondary_key_rotation_index = next_secondary_key_rotation_index + PRIMARY_KEYS_PER_SECONDARY_KEY;
    }

    fmna_update_secondary_index(current_index);

    fmna_gatt_send_command_response(FMNA_SERVICE_OPCODE_COMMAND_RESPONSE, *(uint16_t *)p_event_data, FMNA_SERVICE_OPCODE_CONFIGURE_SEPARATED_STATE, RESPONSE_STATUS_SUCCESS);
}


void fmna_state_machine_set_next_secondary_key_rotation_index(uint16_t conn_handle, uint32_t index)
{
    next_secondary_key_rotation_index = index;
    fmna_queue_platform_evt_put(&conn_handle, sizeof(conn_handle), dispatch_set_next_secondary_key_rotation_index_handler);
}

/// Function for handling Find My state machine events.
/// @param fmna_evt  FIND_MY App event.
/// @param p_context Unused.
void fmna_evt_handler(FMNA_SM_Event_t fmna_evt, void * p_context)
{
    uint32_t ret_code = 0;

    uint16_t list_len = fmna_sm_handlers[m_fmna_state].count;

    bool evtHandled = false;

    FMNA_LOG_INFO("Handle event request: %s", fmna_sm_event_strings[fmna_evt]);

    for (uint16_t i = 0; i < list_len; i++)
    {
        if (fmna_sm_handlers[m_fmna_state].p_array_handlers[i].handler && (fmna_sm_handlers[m_fmna_state].p_array_handlers[i].fmna_evt == fmna_evt))
        {
            evtHandled = true;
            ret_code = fmna_sm_handlers[m_fmna_state].p_array_handlers[i].handler(fmna_evt, p_context);

            // update state (success)
            if ((ret_code == FMNA_SM_STATUS_SUCCESS) &&
                 (fmna_sm_handlers[m_fmna_state].p_array_handlers[i].fmna_state_success != FMNA_SM_NOCHANGE))
            {
                FMNA_LOG_INFO("State machine changed: %s to %s", fmna_sm_state_strings[m_fmna_state], fmna_sm_state_strings[fmna_sm_handlers[m_fmna_state].p_array_handlers[i].fmna_state_success]);
                m_fmna_state = fmna_sm_handlers[m_fmna_state].p_array_handlers[i].fmna_state_success;
            }
            else if ((ret_code != FMNA_SM_STATUS_SUCCESS) && (fmna_sm_handlers[m_fmna_state].p_array_handlers[i].fmna_state_fail != FMNA_SM_NOCHANGE))
            {
                FMNA_LOG_INFO("State machine changed: %s to %s", fmna_sm_state_strings[m_fmna_state], fmna_sm_state_strings[fmna_sm_handlers[m_fmna_state].p_array_handlers[i].fmna_state_fail]);
                m_fmna_state = fmna_sm_handlers[m_fmna_state].p_array_handlers[i].fmna_state_fail;
            }
            break;
        }
    }

    if (evtHandled == false)
    {
        FMNA_LOG_WARNING("Event [%s] was not executed because it is not in the state: [%s]",  fmna_sm_event_strings[fmna_evt], fmna_sm_state_strings[m_fmna_state]);
    }
}

void fmna_state_machine_clear_keys(void)
{
    FMNA_LOG_DEBUG("Clear keys.");
    memset(&m_fmna_current_primary_key, 0, sizeof(m_fmna_current_primary_key));
    memset(&m_fmna_current_separated_primary_key, 0, sizeof(m_fmna_current_separated_primary_key));
    memset(&m_fmna_current_secondary_key, 0, sizeof(m_fmna_current_secondary_key));
    m_current_separated_primary_key_index = 0;
}


// latch current separated key without re-aligning future separated keys
void fmna_state_machine_latch_current_separated_key(uint16_t conn_handle)
{
    uint32_t current_index = m_fmna_current_primary_key.index;
    m_current_separated_primary_key_index = current_index;
    memcpy(&m_fmna_current_separated_primary_key, &m_fmna_current_primary_key, sizeof(fmna_primary_key_t));
    fmna_gatt_send_indication(conn_handle,
                              FMNA_SERVICE_OPCODE_LATCH_SEPARATED_KEY_RESPONSE,
                              &m_current_separated_primary_key_index,
                              sizeof(m_current_separated_primary_key_index));
}

void fmna_update_secondary_index(uint32_t current_index)
{
    uint32_t secondary_index = current_index / PRIMARY_KEYS_PER_SECONDARY_KEY + 1;

    //TODO: Read current secondary key

    if (m_fmna_current_secondary_key.index < secondary_index)
    {
        fmna_ret_code_t ret_code = fmna_crypto_roll_secondary_key();
        FMNA_ERROR_CHECK(ret_code);

        m_fmna_current_secondary_key.index = secondary_index;

        if (m_fmna_current_secondary_key.index != secondary_index)
        {
            FMNA_ERROR_CHECK(FMNA_ERROR_INTERNAL);
        }
    }
    else if (m_fmna_current_secondary_key.index > secondary_index)
    {
        FMNA_ERROR_CHECK(FMNA_ERROR_INTERNAL);
    }
}

bool fmna_state_machine_is_nearby(void) {
    return (m_is_nearby == IS_NEARBY_TRUE);
}

void fmna_state_machine_fmna_active_set(bool is_enable)
{
    if (is_enable)
    {
        if (m_fmna_state == FMNA_SM_PAIR)
        {
            start_pair_adv();
        }
        else if (m_fmna_state == FMNA_SM_NEARBY)
        {
            fmna_disconnecting_evt_nearby_handler(FMNA_SM_EVENT_MOTION_DETECTED , 0);
        }
        else
        {
            fmna_nearby_evt_timeout_handler(FMNA_SM_EVENT_MOTION_DETECTED , 0);
        }
    }
    else
    {
        if(fmna_connection_get_num_connections() != 0)
        {
            fmna_connection_disconnect_all();
        }
        fmna_adv_platform_stop_adv();
    }
}

