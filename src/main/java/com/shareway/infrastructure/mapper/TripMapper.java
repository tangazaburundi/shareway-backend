package com.shareway.infrastructure.mapper;

import com.shareway.application.dto.request.CreateTripRequest;
import com.shareway.application.dto.response.TripResponse;
import com.shareway.domain.model.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TripMapper {
    Trip toEntity(CreateTripRequest request);

    TripResponse toResponse(Trip trip);

}
