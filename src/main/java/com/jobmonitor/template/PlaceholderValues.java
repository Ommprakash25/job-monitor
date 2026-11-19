package com.jobmonitor.template;

/**
 * Values for one recruiter. Blank values fall back: name to "Hiring Team", company to
 * "your company", and role to {@code roleFallback} (Master data, default "Software Engineer").
 */
public record PlaceholderValues(String name, String company, String role, String roleFallback) {
}
