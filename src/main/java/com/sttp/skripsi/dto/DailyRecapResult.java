package com.sttp.skripsi.dto;

import lombok.Data;

@Data
public class DailyRecapResult {
    private Integer backlog_tasks;
    private Integer daily_recaps;
    private String message;
    private Integer project_id;
    private String project_name;
    private String status;
}