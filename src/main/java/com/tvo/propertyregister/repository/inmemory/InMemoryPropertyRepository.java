package com.tvo.propertyregister.repository.inmemory;

import com.tvo.propertyregister.exception.NoSuchOwnerException;
import com.tvo.propertyregister.exception.PropertyNotFoundException;
import com.tvo.propertyregister.model.owner.FamilyStatus;
import com.tvo.propertyregister.model.owner.Owner;
import com.tvo.propertyregister.model.property.Property;
import com.tvo.propertyregister.model.property.PropertyCondition;
import com.tvo.propertyregister.model.property.PropertyType;
import com.tvo.propertyregister.repository.PropertyRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InMemoryPropertyRepository implements PropertyRepository {

    private final Property propertyFirst = new Property(
            1, PropertyType.FLAT, "Prague", "Heroev Street 24",
            70, 3, new BigDecimal("500000"),
            LocalDate.of(2020, 4, 10),
            LocalDate.of(2012, 1, 9),
            PropertyCondition.GOOD);
    private final Property propertySecond = new Property(2, PropertyType.HOUSE, "Prague", "Boris Niemcov Street 220",
            150, 5, new BigDecimal("750000"),
            LocalDate.of(2020, 4, 10),
            LocalDate.of(2012, 1, 9),
            PropertyCondition.GOOD);
    private final Property propertyThird = new Property(3, PropertyType.OFFICE, "Prague", "Evropska 6",
            300, 10, new BigDecimal("1000000"),
            LocalDate.of(2023, 4, 10),
            LocalDate.of(2023, 1, 9),
            PropertyCondition.GOOD);

    private final Owner ownerFirst = new Owner(1, "John", "Smith",
            30, FamilyStatus.SINGLE,
            false, "johnsmith@gmail.com",
            "+456987123",
            LocalDate.of(1994, 8, 9),
            new BigDecimal("0"), new ArrayList<>(List.of(propertyFirst)));
    private final Owner ownerSecond = new Owner(2, "Linda", "Johnson",
            31, FamilyStatus.MARRIED,
            true, "lindajohnson@gmail.com",
            "+789456147",
            LocalDate.of(1993, 7, 17),
            new BigDecimal("0"), new ArrayList<>(List.of(propertySecond)));
    private final Owner ownerThird = new Owner(3, "Dan", "Kravets",
            30, FamilyStatus.SINGLE,
            false, "DDD_JDK@gmail.com",
            "+784578457",
            LocalDate.of(1994, 1, 31),
            new BigDecimal("0"), new ArrayList<>(List.of(propertyThird)));

    private final List<Owner> owners = List.of(ownerFirst, ownerSecond, ownerThird);

    private static int counter = 3;

    @Override
    public List<Property> findAll(int ownerId) {
        for (Owner currentOwner : this.owners) {
            if (currentOwner.getId() == ownerId) {
                return currentOwner.getProperties();
            }
        }

        throw new NoSuchOwnerException("Owner with id: %s does not exists!".formatted(ownerId));
    }

    @Override
    public boolean save(Owner owner, Property property) {
        return false;
    }

    @Override
    public boolean update(int ownerId, List<Property> updatedProperties) {
        return false;
    }
}
