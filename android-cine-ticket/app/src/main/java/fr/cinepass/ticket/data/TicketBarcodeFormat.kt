package fr.cinepass.ticket.data

import com.google.zxing.BarcodeFormat

/**
 * Formats de code-barres supportés à la fois par ZXing (affichage local)
 * et par l'API Google Wallet (champ `barcode.type`).
 */
enum class TicketBarcodeFormat(
    val label: String,
    val zxing: BarcodeFormat,
    val walletType: String,
    /** Les codes 1D sont rendus dans un rectangle large, les 2D dans un carré. */
    val isTwoDimensional: Boolean,
) {
    QR_CODE("QR Code", BarcodeFormat.QR_CODE, "QR_CODE", true),
    AZTEC("Aztec", BarcodeFormat.AZTEC, "AZTEC", true),
    PDF_417("PDF417", BarcodeFormat.PDF_417, "PDF_417", false),
    DATA_MATRIX("Data Matrix", BarcodeFormat.DATA_MATRIX, "QR_CODE", true),
    CODE_128("Code 128", BarcodeFormat.CODE_128, "CODE_128", false),
    CODE_39("Code 39", BarcodeFormat.CODE_39, "CODE_39", false),
    EAN_13("EAN-13", BarcodeFormat.EAN_13, "EAN_13", false),
    ITF("ITF", BarcodeFormat.ITF, "ITF_14", false),
    ;

    companion object {
        fun fromName(name: String?): TicketBarcodeFormat =
            entries.firstOrNull { it.name == name } ?: QR_CODE
    }
}
