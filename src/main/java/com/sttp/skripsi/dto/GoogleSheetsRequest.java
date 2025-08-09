package com.sttp.skripsi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request body for Google Sheets data extraction")
public class GoogleSheetsRequest {
    private String sheetUrl;
    private String sheetId; // Add this field
    
    @Schema(
        description = "URL of the Google Sheet to extract data from",
        example = "https://docs.google.com/spreadsheets/d/YOUR_SHEET_ID",
        required = true
    )
    public String getSheetId() {
        return sheetId;
    }
    
    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }
}