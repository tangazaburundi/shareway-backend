package com.shareway.application.mapper;

import com.shareway.application.dto.response.TripEditHistoryResponse;
import com.shareway.domain.model.TripEditHistory;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TripHistoryMapper {

    TripEditHistoryResponse toResponse(TripEditHistory tripHistory);

}
