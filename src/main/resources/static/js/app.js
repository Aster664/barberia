document.addEventListener("DOMContentLoaded", () => {
    const menuToggle = document.querySelector("[data-menu-toggle]");
    const overlay = document.querySelector("[data-mobile-overlay]");

    const closeMenu = () => document.body.classList.remove("nav-open");

    if (menuToggle) {
        menuToggle.addEventListener("click", () => {
            document.body.classList.toggle("nav-open");
        });
    }

    if (overlay) {
        overlay.addEventListener("click", closeMenu);
    }

    document.querySelectorAll("[data-confirm]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            const message = form.dataset.confirm || "¿Confirmas esta operación?";
            if (!window.confirm(message)) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll("[data-dismiss]").forEach((button) => {
        button.addEventListener("click", () => {
            button.closest(".alert")?.remove();
        });
    });
});
