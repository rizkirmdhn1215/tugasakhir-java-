package com.sttp.skripsi.controller;

import com.sttp.skripsi.dto.TaskDetailResponse;
import com.sttp.skripsi.service.TaskDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskDetailController {
    private final TaskDetailService taskDetailService;

    @GetMapping("/task-detail/{id_task}")
    public ResponseEntity<TaskDetailResponse> getTaskDetail(@PathVariable("id_task") Long taskId) {
        TaskDetailResponse response = taskDetailService.getTaskDetail(taskId);
        return ResponseEntity.ok(response);
    }
} 