package com.devtamuno.kmp.nfcreader.contract

/**
 * NFC Forum URI identifier codes used to expand the first byte of a Well-Known URI NDEF record
 * into its full URI prefix string.
 *
 * Defined in the NFC Data Exchange Format (NDEF) Technical Specification, Section 3.2.2.
 * Shared by both the Android and iOS tag parser implementations.
 */
internal val NFC_URI_PREFIXES = arrayOf(
    "", "http://www.", "https://www.", "http://", "https://",
    "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.",
    "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://",
    "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:", "sip:",
    "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
    "tcpobex://", "irdaobex://", "file://", "urn:epc:id:",
    "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:",
    "urn:nfc:",
)