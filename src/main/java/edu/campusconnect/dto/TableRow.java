package edu.campusconnect.dto;

import java.util.List;

public record TableRow(String url, List<String> cells) {
  public static TableRow of(String url, Object... cells) {
    return new TableRow(
        url, java.util.Arrays.stream(cells).map(x -> x == null ? "—" : String.valueOf(x)).toList());
  }
}
