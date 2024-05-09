

#include "fmna_log_platform.h"
#include <stdarg.h>

#define LOG_LINE_BUF_SIZE           512
#define LOG_PER_LINE_HEX_DUMP_SIZE  8
#define LOG_NEWLINE_SIGN            "\r\n"

#define LOG_LOCK()                  LOCAL_INT_DISABLE(BLE_IRQn) /**< log lock. */
#define LOG_UNLOCK()                LOCAL_INT_RESTORE()         /**< log unlock. */

static fmna_log_output_t  s_fmna_platform_log_output;
static uint8_t            s_log_encode_buf[LOG_LINE_BUF_SIZE];

static const char *s_log_svt_lvl_output_info[] =            /**< App log severity level outpout information. */
{
    [0] = "[FMNA] <Error> ",
    [1] = "[FMNA] <Warn>  ",
    [2] = "[FMNA] <Info>  ",
    [3] = "[FMNA] <Debug> ",
};

static uint16_t log_strcpy(uint16_t wr_idx, uint8_t *p_log_buff, const char *p_log_data)
{
    uint16_t cpy_length = 0;

    if (!p_log_buff || !p_log_data)
    {
        return cpy_length;
    }

    while (*p_log_data != 0)
    {
        if ((wr_idx + cpy_length) < LOG_LINE_BUF_SIZE)
        {
            p_log_buff[wr_idx + cpy_length] = *p_log_data++;
            cpy_length++;
        }
        else
        {
            break;
        }
    }

    return cpy_length;
}

fmna_ret_code_t fmna_log_init(fmna_log_output_t log_output)
{
    #if FMNA_LOG_ENABLE
    if (NULL == log_output)
    {
        return FMNA_ERROR_INVALID_LOG_PARAM;
    }
    #endif

    s_fmna_platform_log_output = log_output;

    return FMNA_SUCCESS;
}

void fmna_log_output(uint8_t level, const char *file, const char *func, const long line, const char *format, ...)
{
    if (NULL == s_fmna_platform_log_output)
    {
        return;
    }

    if (FMNA_LOG_FILTER_LEVEL < level)
    {
        return;
    }

    uint16_t log_length = 0;
    char     line_num[10+1]  = { 0 };
    int      fmt_result     = 0;
    va_list  ap;

    va_start(ap, format);

    LOG_LOCK();

    log_length += log_strcpy(log_length, s_log_encode_buf, s_log_svt_lvl_output_info[level]);

    if (0 == level)
    {
        log_length += log_strcpy(log_length, s_log_encode_buf, "(");
        log_length += log_strcpy(log_length, s_log_encode_buf, file);
        log_length += log_strcpy(log_length, s_log_encode_buf, " :");
        log_length += log_strcpy(log_length, s_log_encode_buf, func);
        log_length += log_strcpy(log_length, s_log_encode_buf, " Line:");
        snprintf(line_num, 10, "%ld", line);
        log_length += log_strcpy(log_length, s_log_encode_buf, line_num);
        log_length += log_strcpy(log_length, s_log_encode_buf, ") ");
    }

    fmt_result = vsnprintf((char *)s_log_encode_buf + log_length, LOG_LINE_BUF_SIZE - log_length, format, ap);

    va_end(ap);

    if ((fmt_result > -1) && (log_length + fmt_result) <= LOG_LINE_BUF_SIZE)
    {
        log_length += fmt_result;
    }
    else
    {
        log_length = LOG_LINE_BUF_SIZE;
    }

    if (log_length + strlen(LOG_NEWLINE_SIGN) > LOG_LINE_BUF_SIZE)
    {
        log_length = LOG_LINE_BUF_SIZE;
        log_length -= strlen(LOG_NEWLINE_SIGN);
    }

    log_length += log_strcpy(log_length, s_log_encode_buf, "\r\n");

    s_fmna_platform_log_output(s_log_encode_buf, log_length);

    LOG_UNLOCK();
}



void fmna_log_hex_dump(void *p_data, uint16_t length)
{
    if (NULL == s_fmna_platform_log_output)
    {
        return;
    }

    #if (FMNA_LOG_FILTER_LEVEL < 3)

    return;

    # else

    uint16_t log_length  = 0;
    uint16_t convert_idx = 0;
    uint16_t line_num    = 0;
    char     dump_str[8] = {0};

    LOG_LOCK();

    line_num = length / LOG_PER_LINE_HEX_DUMP_SIZE;

    for (uint8_t i = 0; i < line_num; i ++)
    {
        for (uint8_t j = 0; j < LOG_PER_LINE_HEX_DUMP_SIZE; j++)
        {
            snprintf(dump_str, 8, "%02X ", ((uint8_t *)p_data)[convert_idx++]);
            log_length += log_strcpy(log_length, s_log_encode_buf, dump_str);
        }

        if (convert_idx % LOG_PER_LINE_HEX_DUMP_SIZE == 0)
        {
            snprintf(dump_str, 8, " | ");
            log_length += log_strcpy(log_length, s_log_encode_buf, dump_str);
            convert_idx -= LOG_PER_LINE_HEX_DUMP_SIZE;
            for (uint8_t j = 0; j < LOG_PER_LINE_HEX_DUMP_SIZE; j++)
            {
                if (((uint8_t *)p_data)[convert_idx] < ' ' || ((uint8_t *)p_data)[convert_idx] > 0x7f)
                {
                    s_log_encode_buf[log_length] = '.';
                    log_length++;
                }
                else
                {
                    s_log_encode_buf[log_length] = ((uint8_t *)p_data)[convert_idx];
                    log_length++;
                }
                convert_idx++;
            }
        }
        log_length += log_strcpy(log_length, s_log_encode_buf, LOG_NEWLINE_SIGN);
        s_fmna_platform_log_output(s_log_encode_buf, log_length);
        log_length = 0;
    }

    if (length % LOG_PER_LINE_HEX_DUMP_SIZE)
    {
        for (uint8_t j = 0; j < length % LOG_PER_LINE_HEX_DUMP_SIZE; j++)
        {
            snprintf(dump_str, 8, "%02X ", ((uint8_t *)p_data)[convert_idx++]);
            log_length += log_strcpy(log_length, s_log_encode_buf, dump_str);
        }

        for (uint8_t j = 0; j < LOG_PER_LINE_HEX_DUMP_SIZE -length % LOG_PER_LINE_HEX_DUMP_SIZE; j++)
        {
            snprintf(dump_str, 8, "   ");
            log_length += log_strcpy(log_length, s_log_encode_buf, dump_str);
        }

        snprintf(dump_str, 8, " | ");
        log_length += log_strcpy(log_length, s_log_encode_buf, dump_str);
        convert_idx -= length % LOG_PER_LINE_HEX_DUMP_SIZE;
        for (uint8_t j = 0; j < length % LOG_PER_LINE_HEX_DUMP_SIZE; j++)
        {
            if (((uint8_t *)p_data)[convert_idx] < ' ' || ((uint8_t *)p_data)[convert_idx] > 0x7f)
            {
                s_log_encode_buf[log_length] = '.';
                log_length++;
            }
            else
            {
                s_log_encode_buf[log_length] = ((uint8_t *)p_data)[convert_idx];
                log_length++;
            }
            convert_idx++;
        }

        log_length += log_strcpy(log_length, s_log_encode_buf, LOG_NEWLINE_SIGN);
        s_fmna_platform_log_output(s_log_encode_buf, log_length);
    }

    LOG_UNLOCK();
    #endif
}

void fmna_error_fault_handler(fmna_error_info_t *p_error_info)
{
    if (NULL == s_fmna_platform_log_output)
    {
        return;
    }

    static char error_print_info[256] = { 0 };

    if (0 == p_error_info->error_type)
    {
        sprintf(error_print_info, "API ret code 0x%04x: No found information.", p_error_info->value.error_code);
    }
    else if (1 == p_error_info->error_type)
    {
        sprintf(error_print_info, "(%s) is not established.", p_error_info->value.expr);
    }

    fmna_log_output(0, p_error_info->file, p_error_info->func, p_error_info->line, "%s", error_print_info);
}
