package com.clinica.backend.mapper;

import com.clinica.backend.dto.LesionPhotoDto;
import com.clinica.backend.model.LesionPhoto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LesionPhotoMapper {

    @Mapping(source = "lesion.id", target = "lesionId")
    LesionPhotoDto toDto(LesionPhoto photo);
}
