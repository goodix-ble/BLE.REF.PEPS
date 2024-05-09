

#ifndef __GR5XXX_ECC_H__
#define __GR5XXX_ECC_H__

#include <wolfssl/wolfcrypt/asn.h>
#include <wolfssl/wolfcrypt/settings.h>
#include <wolfssl/wolfcrypt/types.h>
#include <wolfssl/wolfcrypt/ecc.h>

int gr5xxx_ecc_make_pub_ex(ecc_key* key, int curve_id);
int gr5xxx_ecc_shared_secret(ecc_key* private_key, ecc_key* public_key, uint8_t* out, uint32_t* outlen);
int gr5xxx_ecc_verify_hash(const uint8_t* sig, uint32_t siglen, const uint8_t* hash, uint32_t hashlen, int* res, ecc_key* key);

#endif

