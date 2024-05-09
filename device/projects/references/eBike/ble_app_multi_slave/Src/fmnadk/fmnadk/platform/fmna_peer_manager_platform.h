

#ifndef fmna_peer_manager_platform_h
#define fmna_peer_manager_platform_h


#include "fmna_application.h"
#include "fmna_constants_platform.h"

// Check how many devices we are paired to.
uint32_t fmna_pm_peer_count(void);

/// Delete all BT pairing records.
void fmna_pm_delete_bonds(void);

/// Initializes Peer Manager module.
void fmna_peer_manager_init(void);

#endif /* fmna_peer_manager_h */
