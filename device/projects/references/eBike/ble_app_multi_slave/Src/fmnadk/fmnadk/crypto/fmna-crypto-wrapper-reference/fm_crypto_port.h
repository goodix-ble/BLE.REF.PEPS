

#ifndef FM_CRYPTO_PORT_H
#define FM_CRYPTO_PORT_H

#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>


#include <wolfssl/wolfcrypt/asn.h>
#include <wolfssl/wolfcrypt/settings.h>
#include <wolfssl/wolfcrypt/types.h>
#include <wolfssl/wolfcrypt/ecc.h>

int gr5xxx_ecc_scmult(ecc_point *r, mp_int *s, ecc_point *B, const ecc_set_type *dp);

#endif

