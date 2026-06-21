package com.shareway.application.mapper;

import com.shareway.application.dto.response.TripEditHistoryResponse;
import com.shareway.infrastructure.adapter.audit.domain.model.TripEditHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TripHistoryMapper {

    TripEditHistoryResponse toResponse(TripEditHistory tripHistory);

}
