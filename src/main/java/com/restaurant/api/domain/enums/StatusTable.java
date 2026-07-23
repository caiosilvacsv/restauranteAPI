package com.restaurant.api.domain.enums;

public enum StatusTable {
  AVAILABLE,
  OCCUPIED,
  RESERVED,
  INACTIVE;

  public boolean canTransitionTo(StatusTable nextStatus){
    return switch (this){
      case AVAILABLE, RESERVED -> true;
      case OCCUPIED -> nextStatus ==  AVAILABLE || nextStatus == INACTIVE;
      case INACTIVE -> nextStatus ==  AVAILABLE;
    };
  }

  public boolean isAvailable(){
    return this == AVAILABLE;
  }
}
