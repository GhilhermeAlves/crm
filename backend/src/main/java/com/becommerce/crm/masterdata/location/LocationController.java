package com.becommerce.crm.masterdata.location;

import com.becommerce.crm.masterdata.location.LocationService.*;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Localidades e CEP (doc 19.10 e 20.3). Cadastros globais: leitura para qualquer usuário logado. */
@RestController
@RequestMapping("/api/v1")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @GetMapping("/countries")
    @PreAuthorize("hasAuthority('person:read')")
    public List<Country> countries(@RequestParam(defaultValue = "true") boolean onlyActive) {
        return service.countries(onlyActive);
    }

    @GetMapping("/countries/{id}/states")
    @PreAuthorize("hasAuthority('person:read')")
    public List<State> states(@PathVariable Integer id, @RequestParam(defaultValue = "true") boolean onlyActive) {
        return service.states(id, onlyActive);
    }

    @GetMapping("/states/{id}/cities")
    @PreAuthorize("hasAuthority('person:read')")
    public List<City> cities(@PathVariable Integer id, @RequestParam(required = false) String q,
                             @RequestParam(defaultValue = "true") boolean onlyActive,
                             @RequestParam(defaultValue = "1000") int limit) {
        return service.cities(id, q, onlyActive, limit);
    }

    @GetMapping("/cities/ibge/{ibge}")
    @PreAuthorize("hasAuthority('person:read')")
    public City cityByIbge(@PathVariable int ibge) {
        return service.cityByIbge(ibge);
    }

    @GetMapping("/cities/{id}/neighborhoods")
    @PreAuthorize("hasAuthority('person:read')")
    public List<Neighborhood> neighborhoods(@PathVariable Integer id, @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "true") boolean onlyActive) {
        return service.neighborhoods(id, q, onlyActive);
    }

    @GetMapping("/postal-codes/{cep}")
    @PreAuthorize("hasAuthority('person:read')")
    public PostalCodeResult postalCode(@PathVariable String cep) {
        return service.lookupPostalCode(cep);
    }

    @PostMapping("/countries")
    @PreAuthorize("hasAuthority('location:manage')")
    public Country createCountry(@RequestBody CountryRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.saveCountry(null, req, user.userId());
    }

    @PutMapping("/countries/{id}")
    @PreAuthorize("hasAuthority('location:manage')")
    public Country updateCountry(@PathVariable Integer id, @RequestBody CountryRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.saveCountry(id, req, user.userId());
    }

    @PostMapping("/states")
    @PreAuthorize("hasAuthority('location:manage')")
    public State createState(@RequestBody StateRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.saveState(null, req, user.userId());
    }

    @PutMapping("/states/{id}")
    @PreAuthorize("hasAuthority('location:manage')")
    public State updateState(@PathVariable Integer id, @RequestBody StateRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.saveState(id, req, user.userId());
    }

    @PostMapping("/cities")
    @PreAuthorize("hasAuthority('location:manage')")
    public City createCity(@RequestBody CityRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.saveCity(null, req, user.userId());
    }

    @PutMapping("/cities/{id}")
    @PreAuthorize("hasAuthority('location:manage')")
    public City updateCity(@PathVariable Integer id, @RequestBody CityRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.saveCity(id, req, user.userId());
    }

    @PostMapping("/neighborhoods")
    @PreAuthorize("hasAuthority('person:update') or hasAuthority('location:manage')")
    public Neighborhood createNeighborhood(@RequestBody NeighborhoodRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.saveNeighborhood(null, req, user.userId());
    }

    @PutMapping("/neighborhoods/{id}")
    @PreAuthorize("hasAuthority('location:manage')")
    public Neighborhood updateNeighborhood(@PathVariable UUID id, @RequestBody NeighborhoodRequest req,
                                           @AuthenticationPrincipal CurrentUser user) {
        return service.saveNeighborhood(id, req, user.userId());
    }
}
