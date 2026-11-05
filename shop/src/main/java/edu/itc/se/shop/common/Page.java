package edu.itc.se.shop.common;

import java.util.List;

public record Page<T>(List<T> content, int page, int size, long totalElements) {}