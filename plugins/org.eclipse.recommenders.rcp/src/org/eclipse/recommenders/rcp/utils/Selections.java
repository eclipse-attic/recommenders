package org.eclipse.recommenders.rcp.utils;

import static com.google.common.base.Optional.*;

import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import com.google.common.base.Optional;

public class Selections {

    public static <T> Optional<T> getFirstSelected(OpenEvent e) {
        ISelection selection = e.getViewer().getSelection();
        return getFirstSelected(selection);
    }

    public static <T> Optional<T> getFirstSelected(Viewer viewer) {
        ISelection selection = viewer.getSelection();
        return getFirstSelected(selection);
    }

    public static IStructuredSelection asStructuredSelection(final ISelection selection) {
        return (IStructuredSelection) (isStructured(selection) ? selection : StructuredSelection.EMPTY);
    }

    public static boolean isStructured(final ISelection selection) {
        return selection instanceof IStructuredSelection;
    }

    public static <T> List<T> toList(final ISelection selection) {
        return asStructuredSelection(selection).toList();
    }

    public static <T> Optional<T> safeFirstElement(final ISelection s, final Class<T> type) {
        final Object element = asStructuredSelection(s).getFirstElement();
        return (Optional<T>) (type.isInstance(element) ? of(element) : absent());
    }

    public static <T> Optional<T> getFirstSelected(final ISelection s) {
        final T element = unsafeFirstElement(s);
        return Optional.fromNullable(element);
    }

    public static <T> T unsafeFirstElement(final ISelection s) {
        return (T) asStructuredSelection(s).getFirstElement();
    }

}
