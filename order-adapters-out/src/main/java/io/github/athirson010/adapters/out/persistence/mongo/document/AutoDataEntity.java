package io.github.athirson010.adapters.out.persistence.mongo.document;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AutoDataEntity extends CategorySpecificDataEntity {

    private String vehiclePlate;
    private String vehicleBrand;
    private String vehicleModel;
    private Integer vehicleYear;
    private MoneyEntity vehicleFipeValue;

    public AutoDataEntity(String vehiclePlate, String vehicleBrand, String vehicleModel,
                          Integer vehicleYear, MoneyEntity vehicleFipeValue) {
        super();
        setType("AUTO");
        this.vehiclePlate = vehiclePlate;
        this.vehicleBrand = vehicleBrand;
        this.vehicleModel = vehicleModel;
        this.vehicleYear = vehicleYear;
        this.vehicleFipeValue = vehicleFipeValue;
    }
}
