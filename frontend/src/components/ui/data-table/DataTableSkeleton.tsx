"use client";

import { Skeleton } from "@/components/ui/skeleton";
import { TableCell, TableRow } from "@/components/ui/table";

type DataTableSkeletonProps = {
  columns: number;
  loadingRows: number;
  hasActions: boolean;
};

export function DataTableSkeleton({ columns, loadingRows, hasActions }: DataTableSkeletonProps) {
  return (
    <>
      {Array.from({ length: loadingRows }).map((_, rIdx) => (
        <TableRow key={`skeleton-row-${rIdx}`} className="hover:bg-transparent">
          {Array.from({ length: columns }).map((_, cIdx) => (
            <TableCell key={`skeleton-cell-${cIdx}`} className="p-3">
              <Skeleton className="h-5 w-full max-w-[140px] rounded" />
            </TableCell>
          ))}
          {hasActions && (
            <TableCell className="p-3 text-right">
              <Skeleton className="ml-auto h-7 w-7 rounded-md" />
            </TableCell>
          )}
        </TableRow>
      ))}
    </>
  );
}
