package org.bigcraft.infpoints.command;

import lombok.AccessLevel;
import lombok.Getter;
import org.bigcraft.infpoints.core.Point;

public abstract class PersonalCommand {

    @Getter(AccessLevel.PROTECTED) private final Point point;

    public PersonalCommand(Point point) {
        this.point = point;
        register();
    }

    public abstract void register();
    public abstract void unregister();

}
