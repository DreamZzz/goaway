package com.goaway.contexts.weekly.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GenerateWeeklyRequest {

    @NotBlank(message = "请先填写本周工作碎片")
    @Size(max = 4000, message = "输入过长")
    private String fragments;

    public String getFragments() { return fragments; }
    public void setFragments(String fragments) { this.fragments = fragments; }
}
