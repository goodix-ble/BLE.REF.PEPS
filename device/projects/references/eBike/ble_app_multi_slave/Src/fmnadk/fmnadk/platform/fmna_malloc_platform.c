
#include "fmna_malloc_platform.h"


static fmna_malloc_t     s_malloc;
static fmna_realloc_t    s_realloc;
static fmna_free_t       s_free;

fmna_ret_code_t fmna_malloc_platform_init( fmna_malloc_t malloc, fmna_realloc_t realloc, fmna_free_t free)
{
    if (NULL == malloc ||
        NULL == realloc ||
        NULL == free)
    {
        return FMNA_ERROR_INVALID_STORAGE_PARAM;
    }

    s_malloc  = malloc;
    s_realloc = realloc;
    s_free    = free;
 
    return FMNA_SUCCESS;
}

void fmna_free(void *ptr)
{
    if (s_free)
    {
        s_free(ptr);
    }
}

void * fmna_malloc(size_t size)
{
    if (s_malloc)
    {
        return s_malloc(size);
    }

    return NULL;
}

void * fmna_realloc(void *ptr, size_t size)
{
    if (s_realloc)
    {
        return s_realloc(ptr, size);
    }

    return NULL;
}
