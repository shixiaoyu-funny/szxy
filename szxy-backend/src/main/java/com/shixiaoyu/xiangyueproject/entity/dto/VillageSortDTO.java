package com.shixiaoyu.xiangyueproject.entity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VillageSortDTO {
    @JsonAlias({"village_base", "villageBase"})
    private VillageBase villageBase;
    private Integer likes;
}
