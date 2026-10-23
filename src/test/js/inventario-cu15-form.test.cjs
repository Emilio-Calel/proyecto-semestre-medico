// Ejecutar: node --test src/test/js/inventario-cu15-form.test.cjs
// Evalúa el script real del formulario; DOM mínimo para comprobar cambios de campos y proyección.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const html = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/farmacia/inventario/nuevo.html'), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];

function formulario({ stock = 10, minimo = 5 } = {}) {
    const elementos = new Map();
    function elemento(id) {
        if (!elementos.has(id)) {
            const clases = new Set(['hidden']);
            elementos.set(id, {
                value: '', textContent: '', className: '', required: false, disabled: false,
                listeners: {},
                classList: {
                    add: c => clases.add(c), remove: c => clases.delete(c), contains: c => clases.has(c),
                    toggle(c, activo) { if (activo) clases.add(c); else clases.delete(c); }
                },
                addEventListener(evento, funcion) { this.listeners[evento] = funcion; }
            });
        }
        return elementos.get(id);
    }
    elemento('inventarioId').options = [{ dataset: {} }, { dataset: { stock: String(stock), minimo: String(minimo), nombre: 'Medicamento prueba' } }];
    elemento('inventarioId').selectedIndex = 1;
    vm.runInNewContext(script, { document: { getElementById: elemento } });
    return {
        elemento,
        cambiar(tipo, cantidad) {
            elemento('tipoMovimiento').value = tipo;
            elemento('cantidad').value = String(cantidad);
            elemento('tipoMovimiento').listeners.change();
        }
    };
}

for (const tipo of ['COMPRA', 'DEVOLUCION', 'AJUSTE_POSITIVO', 'VENTA', 'RECLAMO', 'AJUSTE_NEGATIVO']) {
    test(`${tipo}: dirección y proyección de stock`, () => {
        const f = formulario(); f.cambiar(tipo, 2);
        const entrada = ['COMPRA', 'DEVOLUCION', 'AJUSTE_POSITIVO'].includes(tipo);
        assert.equal(f.elemento('stockActual').textContent, 10);
        assert.equal(f.elemento('stockMinimo').textContent, 5);
        assert.equal(f.elemento('stockProyectado').textContent, entrada ? 12 : 8);
        assert.equal(f.elemento('direccionMovimiento').textContent, entrada ? 'Entrada' : 'Salida');
    });
}

test('campos condicionales: ajustes 10–500; devolución y reclamo obligatorios; compra opcional; venta oculta', () => {
    const f = formulario();
    for (const tipo of ['AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO', 'DEVOLUCION', 'RECLAMO']) {
        f.cambiar(tipo, 1);
        assert.equal(f.elemento('motivo').required, true);
        assert.equal(f.elemento('motivo').minLength, tipo.startsWith('AJUSTE') ? 10 : 1);
        assert.equal(f.elemento('motivo').disabled, false);
        assert.equal(f.elemento('campoMotivo').classList.contains('hidden'), false);
        assert.equal(f.elemento('costoUnitario').required, false);
        assert.equal(f.elemento('costoUnitario').disabled, true);
    }
    f.cambiar('COMPRA', 1);
    assert.equal(f.elemento('costoUnitario').required, true);
    assert.equal(f.elemento('costoUnitario').disabled, false);
    assert.equal(f.elemento('campoCosto').classList.contains('hidden'), false);
    assert.equal(f.elemento('motivo').required, false);
    assert.equal(f.elemento('motivo').minLength, 0);
    assert.equal(f.elemento('campoMotivo').classList.contains('hidden'), false);
    f.cambiar('VENTA', 1);
    assert.equal(f.elemento('campoMotivo').classList.contains('hidden'), true);
    assert.equal(f.elemento('motivo').disabled, true);
    assert.equal(f.elemento('motivo').required, false);
});

test('alerta preventiva para entradas y salidas bajo mínimo; al llegar al mínimo no advierte', () => {
    const salida = formulario(); salida.cambiar('VENTA', 8);
    assert.equal(salida.elemento('alertaStock').textContent, 'Medicamento prueba: Stock bajo — disponible: 2 (mínimo: 5)');
    salida.cambiar('VENTA', 5); assert.equal(salida.elemento('alertaStock').textContent, '');
    const entrada = formulario({ stock: 0, minimo: 5 }); entrada.cambiar('COMPRA', 2);
    assert.equal(entrada.elemento('alertaStock').textContent, 'Medicamento prueba: Stock bajo — disponible: 2 (mínimo: 5)');
});

test('stock insuficiente conserva mensaje esperado y muestra saldo negativo sin ocultar el stock actual', () => {
    const f = formulario(); f.cambiar('VENTA', 11);
    assert.equal(f.elemento('stockProyectado').textContent, -1);
    assert.equal(f.elemento('stockActual').textContent, 10);
    assert.equal(f.elemento('alertaStock').textContent, 'Stock insuficiente. El stock actual es 10 unidades.');
});

test('cantidad decimal, vacía, no positiva o fuera de capacidad no produce proyección engañosa', () => {
    const f = formulario();
    for (const cantidad of ['', 0, -1, '1.5', 'texto', '2147483648']) {
        f.cambiar('VENTA', cantidad);
        assert.equal(f.elemento('stockProyectado').textContent, '-');
        assert.equal(f.elemento('alertaStock').textContent, '');
    }
    f.cambiar('COMPRA', 2147483647);
    assert.equal(f.elemento('alertaStock').textContent, 'El stock resultante excede la capacidad permitida.');
});

test('cambiar inventario recalcula y quitar la selección limpia el panel', () => {
    const f = formulario(); f.cambiar('VENTA', 2);
    const inventario = f.elemento('inventarioId');
    inventario.options[1].dataset.stock = '7'; inventario.listeners.change();
    assert.equal(f.elemento('stockActual').textContent, 7);
    assert.equal(f.elemento('stockProyectado').textContent, 5);
    inventario.selectedIndex = 0; inventario.listeners.change();
    for (const id of ['stockActual', 'stockMinimo', 'stockProyectado']) assert.equal(f.elemento(id).textContent, '-');
    assert.equal(f.elemento('alertaStock').textContent, '');
});
