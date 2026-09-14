import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { DataTable, type ColumnDef } from "./data-table";

type TestItem = {
  id: string;
  name: string;
  email: string;
  status: string;
};

const mockData: TestItem[] = [
  { id: "1", name: "Ana Clara", email: "ana@empresa.com", status: "Ativo" },
  { id: "2", name: "Bruno Costa", email: "bruno@empresa.com", status: "Inativo" },
];

const mockColumns: ColumnDef<TestItem>[] = [
  { header: "Nome", accessor: "name" },
  { header: "E-mail", accessor: "email" },
  { header: "Status", accessor: (row) => <span>{row.status}</span> },
];

describe("DataTable Component", () => {
  it("renders table headers and rows accurately", () => {
    render(<DataTable data={mockData} columns={mockColumns} />);

    expect(screen.getByText("Nome")).toBeTruthy();
    expect(screen.getByText("E-mail")).toBeTruthy();
    expect(screen.getByText("Status")).toBeTruthy();
    expect(screen.getByText("Ana Clara")).toBeTruthy();
    expect(screen.getByText("ana@empresa.com")).toBeTruthy();
    expect(screen.getByText("Bruno Costa")).toBeTruthy();
  });

  it("renders empty state message when data is empty", () => {
    render(
      <DataTable
        data={[]}
        columns={mockColumns}
        emptyState={{
          title: "Nenhum cliente cadastrado",
          description: "Adicione o primeiro cliente para começar.",
        }}
      />,
    );

    expect(screen.getByText("Nenhum cliente cadastrado")).toBeTruthy();
    expect(screen.getByText("Adicione o primeiro cliente para começar.")).toBeTruthy();
  });

  it("renders loading skeletons when isLoading is true", () => {
    const { container } = render(
      <DataTable data={[]} columns={mockColumns} isLoading loadingRows={3} />,
    );

    // Skeletons should be present
    const skeletons = container.querySelectorAll(".animate-pulse");
    expect(skeletons.length).toBeGreaterThan(0);
    // Data should not be displayed
    expect(screen.queryByText("Ana Clara")).toBeNull();
  });

  it("fires onRowClick handler when a row is clicked", () => {
    const handleRowClick = vi.fn();
    render(<DataTable data={mockData} columns={mockColumns} onRowClick={handleRowClick} />);

    fireEvent.click(screen.getByText("Ana Clara"));
    expect(handleRowClick).toHaveBeenCalledWith(mockData[0]);
  });

  it("renders sortable headers and triggers onSort", () => {
    const handleSort = vi.fn();
    const sortableCols: ColumnDef<TestItem>[] = [
      { header: "Nome", accessor: "name", sortable: true, sortKey: "name" },
      { header: "E-mail", accessor: "email" },
    ];

    render(
      <DataTable
        data={mockData}
        columns={sortableCols}
        sort={{ column: "name", direction: "asc", onSort: handleSort }}
      />,
    );

    const sortBtn = screen.getByText("Nome");
    fireEvent.click(sortBtn);
    expect(handleSort).toHaveBeenCalledWith("name");
  });

  it("renders pagination and calls onPageChange when clicking next page", () => {
    const handlePageChange = vi.fn();

    render(
      <DataTable
        data={mockData}
        columns={mockColumns}
        pagination={{
          currentPage: 1,
          totalPages: 3,
          totalItems: 30,
          pageSize: 10,
          onPageChange: handlePageChange,
        }}
      />,
    );

    expect(screen.getByText(/Mostrando/)).toBeTruthy();
    expect(screen.getByText(/30/)).toBeTruthy();

    const nextBtn = screen.getByTitle("Próxima página");
    fireEvent.click(nextBtn);
    expect(handlePageChange).toHaveBeenCalledWith(2);
  });
});
