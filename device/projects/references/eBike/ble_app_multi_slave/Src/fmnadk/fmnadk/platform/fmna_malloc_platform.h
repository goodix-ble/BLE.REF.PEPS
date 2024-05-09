

#ifndef fmna_malloc_platform_h
#define fmna_malloc_platform_h

#include "fmna_application.h"

fmna_ret_code_t fmna_malloc_platform_init( fmna_malloc_t  malloc,
                                           fmna_realloc_t realloc,
                                           fmna_free_t    free);
void fmna_free(void *ptr);
void * fmna_malloc(size_t size);
void * fmna_realloc(void *ptr, size_t size);

#endif /* fmna_malloc_platform_h */
