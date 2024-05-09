
#ifndef fmna_platform_log_h
#define fmna_platform_log_h

#include "fmna_application.h"


#define FMNA_LOG_ENABLE                   1
#define FMNA_LOG_FILTER_LEVEL             2

#if FMNA_LOG_ENABLE

#if (FMNA_LOG_FILTER_LEVEL == 0)
#define FMNA_LOG_ERROR(...)                         fmna_log_output(0, __FILE__, __FUNCTION__, __LINE__, __VA_ARGS__)
#define FMNA_LOG_WARNING( ...)
#define FMNA_LOG_INFO(...) 
#define FMNA_LOG_DEBUG(...)
#define FMNA_LOG_HEXDUMP_DEBUG(p_data, len)

#elif (FMNA_LOG_FILTER_LEVEL == 1)
#define FMNA_LOG_ERROR(...)                         fmna_log_output(0, __FILE__, __FUNCTION__, __LINE__, __VA_ARGS__)
#define FMNA_LOG_WARNING( ...)                      fmna_log_output(1, __FILE__, __FUNCTION__, __LINE__, __VA_ARGS__)
#define FMNA_LOG_INFO(...)
#define FMNA_LOG_DEBUG(...)
#define FMNA_LOG_HEXDUMP_DEBUG(p_data, len)

#elif (FMNA_LOG_FILTER_LEVEL == 2)
#define FMNA_LOG_ERROR(...)                         fmna_log_output(0, __FILE__, __FUNCTION__, __LINE__, __VA_ARGS__)
#define FMNA_LOG_WARNING( ...)                      fmna_log_output(1, __FILE__, __FUNCTION__, __LINE__, __VA_ARGS__)
#define FMNA_LOG_INFO(...)                          fmna_log_output(2, __FILE__, __FUNCTION__, __LINE__, __VA_ARGS__)
#define FMNA_LOG_DEBUG(...)
#define FMNA_LOG_HEXDUMP_DEBUG(p_data, len)

#else
#define FMNA_LOG_ERROR(...)                         fmna_log_output(0, __FILE__, __FUNCTION__, __LINE__, __VA_ARGS__)
#define FMNA_LOG_WARNING( ...)                      fmna_log_output(1, __FILE__, __FUNCTION__, __LINE__, __VA_ARGS__)
#define FMNA_LOG_INFO(...)                          fmna_log_output(2, __FILE__, __FUNCTION__, __LINE__, __VA_ARGS__)
#define FMNA_LOG_DEBUG(...)                         fmna_log_output(3, __FILE__, __FUNCTION__, __LINE__, __VA_ARGS__)
#define FMNA_LOG_HEXDUMP_DEBUG(p_data, len)         fmna_log_hex_dump(p_data, len)

#endif


#else

#define FMNA_LOG_ERROR(...)
#define FMNA_LOG_WARNING(...)
#define FMNA_LOG_INFO(...)
#define FMNA_LOG_DEBUG(...)
#define FMNA_LOG_HEXDUMP_DEBUG(p_data, len)
#define FMNA_ERROR_CHECK(ERR_CODE)                  UNUSED(ERR_CODE)
#define FMNA_BOOL_CHECK(EXPR)                       UNUSED(EXPR)
#define FMNA_ASSERT(EXPR)                           UNUSED(EXPR)

#endif

#define FMNA_ERROR_CHECK(ERR_CODE)                  \
    do                                              \
    {                                               \
        if (ERR_CODE != 0)                          \
        {                                           \
            fmna_error_info_t error_info =          \
            {                                       \
                .error_type       = 0,              \
                .value.error_code = ERR_CODE,       \
                .file             = __FILE__,       \
                .func             = __FUNCTION__,   \
                .line             = __LINE__,       \
            };                                      \
            fmna_error_fault_handler(&error_info);  \
        }                                           \
    } while(0)
#define FMNA_BOOL_CHECK(EXPR)                       \
    do                                              \
    {                                               \
        if (!EXPR)                                  \
        {                                           \
            fmna_error_info_t error_info =          \
            {                                       \
                .error_type = 1,                    \
                .value.expr = #EXPR,                \
                .file       = __FILE__,             \
                .func       = __FUNCTION__,         \
                .line       = __LINE__,             \
            };                                      \
            fmna_error_fault_handler(&error_info);  \
        }                                           \
    } while(0)

#define FMNA_ASSERT(EXPR)                           \
    do                                              \
    {                                               \
        fmna_error_info_t error_info =              \
        {                                           \
            .error_type = 1,                        \
            .value.expr = #EXPR,                    \
            .file       = __FILE__,                 \
            .func       = __FUNCTION__,             \
            .line       = __LINE__,                 \
        };                                          \
        fmna_error_fault_handler(&error_info);      \
    } while(0)

typedef struct
{
    uint8_t          error_type;
    union
    {
        uint16_t     error_code;
        char const  *expr;
    } value;
    char const      *file;
    char const      *func;
    uint32_t         line;
} fmna_error_info_t;

fmna_ret_code_t fmna_log_init(fmna_log_output_t log_output);
void fmna_log_output(uint8_t level, const char *file, const char *func, const long line, const char *format, ...);
void fmna_log_hex_dump(void *p_data, uint16_t length);
void fmna_error_fault_handler(fmna_error_info_t *p_error_info);

#endif

