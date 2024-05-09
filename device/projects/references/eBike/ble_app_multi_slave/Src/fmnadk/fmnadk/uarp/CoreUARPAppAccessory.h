/*
 * Disclaimer: IMPORTANT: This Apple software is supplied to you, by Apple Inc. ("Apple"), in your
 * capacity as a current, and in good standing, Licensee in the MFi Licensing Program. Use of this
 * Apple software is governed by and subject to the terms and conditions of your MFi License,
 * including, but not limited to, the restrictions specified in the provision entitled "Public
 * Software", and is further subject to your agreement to the following additional terms, and your
 * agreement that the use, installation, modification or redistribution of this Apple software
 * constitutes acceptance of these additional terms. If you do not agree with these additional terms,
 * you may not use, install, modify or redistribute this Apple software.
 *
 * Subject to all of these terms and in consideration of your agreement to abide by them, Apple grants
 * you, for as long as you are a current and in good-standing MFi Licensee, a personal, non-exclusive
 * license, under Apple's copyrights in this Apple software (the "Apple Software"), to use,
 * reproduce, and modify the Apple Software in source form, and to use, reproduce, modify, and
 * redistribute the Apple Software, with or without modifications, in binary form, in each of the
 * foregoing cases to the extent necessary to develop and/or manufacture "Proposed Products" and
 * "Licensed Products" in accordance with the terms of your MFi License. While you may not
 * redistribute the Apple Software in source form, should you redistribute the Apple Software in binary
 * form, you must retain this notice and the following text and disclaimers in all such redistributions
 * of the Apple Software. Neither the name, trademarks, service marks, or logos of Apple Inc. may be
 * used to endorse or promote products derived from the Apple Software without specific prior written
 * permission from Apple. Except as expressly stated in this notice, no other rights or licenses,
 * express or implied, are granted by Apple herein, including but not limited to any patent rights that
 * may be infringed by your derivative works or by other works in which the Apple Software may be
 * incorporated. Apple may terminate this license to the Apple Software by removing it from the list
 * of Licensed Technology in the MFi License, or otherwise in accordance with the terms of such MFi License.
 *
 * Unless you explicitly state otherwise, if you provide any ideas, suggestions, recommendations, bug
 * fixes or enhancements to Apple in connection with this software ("Feedback"), you hereby grant to
 * Apple a non-exclusive, fully paid-up, perpetual, irrevocable, worldwide license to make, use,
 * reproduce, incorporate, modify, display, perform, sell, make or have made derivative works of,
 * distribute (directly or indirectly) and sublicense, such Feedback in connection with Apple products
 * and services. Providing this Feedback is voluntary, but if you do provide Feedback to Apple, you
 * acknowledge and agree that Apple may exercise the license granted above without the payment of
 * royalties or further consideration to Participant.
 * The Apple Software is provided by Apple on an "AS IS" basis. APPLE MAKES NO WARRANTIES, EXPRESS OR
 * IMPLIED, INCLUDING WITHOUT LIMITATION THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND OPERATION ALONE OR
 * IN COMBINATION WITH YOUR PRODUCTS.
 *
 * IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION, MODIFICATION
 * AND/OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED AND WHETHER UNDER THEORY OF CONTRACT, TORT
 * (INCLUDING NEGLIGENCE), STRICT LIABILITY OR OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright (C) 2020 Apple Inc. All Rights Reserved.
 */

#ifndef FMNA_UARP_APP_h
#define FMNA_UARP_APP_h

#include "CoreUARPPlatform.h"
#include "CoreUARPUtils.h"
#include "CoreUARPAccessory.h"
#include "CoreUARPProtocolDefines.h"
#include "CoreUARPPlatformAccessory.h"

#define kUARPAppInfoLength            128
#define kUARPAppMaxTxMsgPayloadSize   247
#define kUARPAppMaxRxMsgPayloadSize   247
#define kUARPAppPayloadWindowSize     1024

#define kUARPAppSuperBinaryPayloadIndexProto        0
#define kUARPAppSuperBinaryPayloadIndexEVT          1
#define kUARPAppSuperBinaryPayloadIndexDVT          2
#define kUARPAppSuperBinaryPayloadIndexPVT          3

#define kUARPAppEventBinarySize          1
#define kUARPAppEventBinaryFragment      2
#define kUARPAppEventBinaryComplete      3
#define kUARPAppEventBinaryApply         4

struct UARPAppAsset
{
    struct uarpPlatformAsset *pAsset;

    struct UARPAppController *pController;
};

typedef struct
{
    uint8_t   *p_data;
    uint32_t   offset;
    uint32_t   length;
} UARPAppBinaryFragment;

typedef struct
{
    uint8_t  evt_id;
    union
    {
        uint32_t                BinaryFileSize;
        UARPAppBinaryFragment   BinaryFragment;
    } param;
}UARPAppEvent_t;

typedef void (*fcnUARPAppEventHandler)(UARPAppEvent_t *p_evt);

struct UARPAppCallbacks
{
    fcnUarpAccessorySendMessage     fSendMessage;
    fcnUARPAppEventHandler   fUarpEventHandler;
};

struct UARPAppAccessory
{
    struct uarpPlatformAccessory _accessory;

    char manufacturerName[kUARPAppInfoLength];
    char modelName[kUARPAppInfoLength];
    char serialNumber[kUARPAppInfoLength];
    char hardwareVersion[kUARPAppInfoLength];

    struct UARPVersion activeFirmwareVersion;
    struct UARPVersion stagedFirmwareVersion;

    struct UARPAppCallbacks callbacks;

    uint32_t lastAction;
    uint32_t lastActionStatus;

    struct UARPAppAsset pSuperBinary;
    UARPBool hasPayload;
};

struct UARPAppController
{
    struct uarpPlatformController _controller;

    void *pDelegate;

    uint8_t *pBuffer;

    // very simple queue
    // Pending buffer since UARP could have up to 2 tx packets at the same time
    uint8_t *pPendingBuffer;
    uint16_t pendingLength;
};

uint32_t UARPAppInit( struct UARPAppAccessory *pAccessory,
                       const char *manufacturerName,
                       const char *modelName,
                       const char *serialNumber,
                       const char *hardwareVersion,
                       struct UARPVersion *pActiveFirmwareVersion,
                       struct UARPVersion *pStagedFirmwareVersion,
                       struct UARPAppCallbacks *pCallbacks );

uint32_t UARPAppControllerAdd( struct UARPAppAccessory *pAccessory,
                                struct UARPAppController *pController );

uint32_t UARPAppControllerRemove( struct UARPAppAccessory *pAccessory,
                                   struct UARPAppController *pController );

uint32_t UARPAppRecvMessage( struct UARPAppAccessory *pAccessory,
                              struct UARPAppController *pController,
                              uint8_t *pBuffer, uint32_t length );

uint32_t UARPAppSendMessageComplete( void *pAccessoryDelegate,
                                       void *pControllerDelegate);

void UARPAppApplyStagedAssetsPendingHandle( void );

#endif /* UARPAppUARP_h */
