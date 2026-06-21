package com.shareway.infrastructure.mapper;

import com.shareway.application.dto.request.CreateTripRequest;
import com.shareway.application.dto.response.TripResponse;
import com.shareway.infrastructure.adapter.audit.domain.model.Trip;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TripMapper {
    Trip toEntity(CreateTripRequest request);

    TripResponse toResponse(Trip trip);

}
