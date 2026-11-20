package edu.campusconnect.util;

import edu.campusconnect.dto.TableRow;
import java.util.*;

public final class Csv {
  private Csv() {}

  public static String cell(String v) {
    String s = v == null ? "" : v;
    if (s.stripLeading().matches("^[=+@-].*") || s.startsWith("\t") || s.startsWith("\r"))
      s = "'" + s;
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }

  public static String export(List<String> headers, List<TableRow> rows) {
    var b = new StringBuilder("\ufeff");
    b.append(headers.stream().map(Csv::cell).collect(java.util.stream.Collectors.joining(",")))
        .append("\r\n");
    for (var r : rows)
      b.append(r.cells().stream().map(Csv::cell).collect(java.util.stream.Collectors.joining(",")))
          .append("\r\n");
    return b.toString();
  }
}
