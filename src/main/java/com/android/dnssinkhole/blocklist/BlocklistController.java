package com.android.dnssinkhole.blocklist;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/blocklist")
class BlocklistController {

    private final BlocklistService blocklistService;

    BlocklistController(BlocklistService blocklistService) {
        this.blocklistService = blocklistService;
    }

    @GetMapping("/check")
    Map<String, Object> checkDomain(@RequestParam String domain) {
        return Map.of("domain", domain, "blocked", blocklistService.isBlocked(domain));
    }

    @PostMapping("/reload")
    Map<String, Object> reloadBlocklist() {
        return Map.of("staticBlocklistSize", blocklistService.reloadStaticList());
    }
}
