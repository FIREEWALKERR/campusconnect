package edu.campusconnect.controller;

import edu.campusconnect.dto.TableRow;
import edu.campusconnect.util.Csv;
import java.util.*;
import org.springframework.http.*;
import org.springframework.ui.Model;

final class TableSupport {
  private TableSupport() {}

  static String show(
      Model m,
      String title,
      List<String> headers,
      List<TableRow> rows,
      int page,
      String q,
      String create,
      String export) {
    int pages = Math.max(1, (rows.size() + 14) / 15);
    int index = Math.max(0, Math.min(page, pages - 1));
    m.addAttribute("title", title);
    m.addAttribute("headers", headers);
    m.addAttribute("rows", rows.subList(index * 15, Math.min(rows.size(), (index + 1) * 15)));
    m.addAttribute("pageIndex", index);
    m.addAttribute("pageCount", pages);
    m.addAttribute("total", rows.size());
    m.addAttribute("q", q);
    m.addAttribute("createUrl", create);
    m.addAttribute("exportUrl", export);
    return "table";
  }

  static ResponseEntity<String> csv(String name, List<String> headers, List<TableRow> rows) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + name + ".csv")
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .cacheControl(CacheControl.noStore())
        .body(Csv.export(headers, rows));
  }
}
