import { describe, it, expect } from "vitest";
import { mapCreateTenantRequest, mapUpdateTenantRequest } from "./tenant.service";
import type { CreateTenantRequest, UpdateTenantRequest } from "../types/tenant.types";

function makeCreateData(overrides: Partial<CreateTenantRequest> = {}): CreateTenantRequest {
  return {
    legalName: "Razão Social Teste",
    tradingName: "Fantasia Teste",
    cnpj: "12.345.678/0001-90",
    stateRegistration: "",
    municipalRegistration: "",
    email: "empresa@teste.com",
    phone: "(11) 99999-9999",
    website: "",
    status: "active",
    plan: "starter",
    maxUsers: 5,
    maxStorageMb: 1024,
    maxContacts: 500,
    logoUrl: null,
    notes: "",
    address: {
      zipCode: "12345-678",
      street: "Rua Teste",
      number: "123",
      complement: "",
      neighborhood: "Centro",
      city: "São Paulo",
      state: "SP",
      country: "Brasil",
    },
    ...(overrides as Partial<CreateTenantRequest>),
  } as CreateTenantRequest;
}

describe("mapCreateTenantRequest", () => {
  it("achata o address aninhado para o wire-format (address* achatado)", () => {
    const wire = mapCreateTenantRequest(makeCreateData());
    expect(wire).toMatchObject({
      addressZipCode: "12345-678",
      addressStreet: "Rua Teste",
      addressNumber: "123",
      addressComplement: "",
      addressNeighborhood: "Centro",
      addressCity: "São Paulo",
      addressState: "SP",
      addressCountry: "Brasil",
    });
    // Não envia `address` aninhado (causa do 400)
    expect("address" in wire).toBe(false);
  });

  it("envia cnpj/cep/telefone como preenchidos no formulário (pass-through)", () => {
    const wire = mapCreateTenantRequest(makeCreateData());
    expect(wire.cnpj).toBe("12.345.678/0001-90");
    expect(wire.addressZipCode).toBe("12345-678");
    expect(wire.phone).toBe("(11) 99999-9999");
    expect(wire.email).toBe("empresa@teste.com");
  });

  it("envia plan em UPPERCASE e NÃO envia status no create (contrato não tem status)", () => {
    const wire = mapCreateTenantRequest(makeCreateData());
    expect(wire.plan).toBe("STARTER");
    expect("status" in wire).toBe(false);
  });

  it("aplica defaults para campos opcionais vazios", () => {
    const data = makeCreateData({
      website: "",
      logoUrl: null,
      address: { ...makeCreateData().address, complement: "" },
    });
    const wire = mapCreateTenantRequest(data);
    expect(wire.website).toBe("");
    expect(wire.stateRegistration).toBe("");
    expect(wire.municipalRegistration).toBe("");
    expect(wire.notes).toBe("");
    expect(wire.logoUrl).toBeNull();
    expect(wire.addressComplement).toBe("");
    expect(wire.addressCountry).toBe("Brasil");
  });

  it("propaga maxUsers/maxStorageMb/maxContacts", () => {
    const wire = mapCreateTenantRequest(makeCreateData());
    expect(wire.maxUsers).toBe(5);
    expect(wire.maxStorageMb).toBe(1024);
    expect(wire.maxContacts).toBe(500);
  });
});

describe("mapUpdateTenantRequest", () => {
  it("envia apenas campos definidos e achata address quando presente", () => {
    const data: UpdateTenantRequest = {
      plan: "professional",
      status: "suspended",
      address: { ...makeCreateData().address, city: "Campinas" },
    };
    const wire = mapUpdateTenantRequest(data);
    expect(wire).toEqual({
      plan: "PROFESSIONAL",
      status: "SUSPENDED",
      addressZipCode: "12345-678",
      addressStreet: "Rua Teste",
      addressNumber: "123",
      addressComplement: "",
      addressNeighborhood: "Centro",
      addressCity: "Campinas",
      addressState: "SP",
      addressCountry: "Brasil",
    });
  });

  it("não envia address quando não informado (payload parcial)", () => {
    const wire = mapUpdateTenantRequest({ phone: "(11) 98888-8888" });
    expect(wire).toEqual({ phone: "(11) 98888-8888" });
    expect("addressZipCode" in wire).toBe(false);
  });
});
