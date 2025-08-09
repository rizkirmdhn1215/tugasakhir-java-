package com.sttp.skripsi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response from Google Sheets data extraction")
public class GoogleSheetsResponse {
    @Schema(description = "Process ID for tracking the extraction")
    private String processId;

    @Schema(description = "Status message")
    private String message;

    @Schema(description = "Number of daily recaps processed", example = "5")
    private DailyRecapResult dailyRecaps;

    @Schema(description = "Sheet ID")
    private String sheetId;

    @Schema(description = "Number of rows processed")
    private Integer rowsProcessed;

    @Schema(description = "Number of columns processed")
    private Integer columnsProcessed;

    @Schema(description = "Processing status")
    private String status;

    public GoogleSheetsResponse(String processId, String message) {
        this.processId = processId;
        this.message = message;
    }
}