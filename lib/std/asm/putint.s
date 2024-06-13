.section .text
.global _putint
_putint:
	sd fp, 16(sp)
	mv fp, sp
	sd ra, 24(fp)
	sd a2, 32(fp)
	sd a3, 40(fp)
	sd a4, 48(fp)
	add sp, fp, 48
	j L18
L18:
L4:
	mv a3, fp
	li a2, 8
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	ld a2, 0(a2)
	mv a3, a2
	li a2, 0
	mv a2, a2
	slt a2, a3, a2
	mv a2, a2
	bnez a2, L6
L20:
	j L7
L6:
	li a2, 0
	sd a2, 0(sp)
	li a2, 45
	sd a2, 8(sp)
	call _putchar
	ld a2, 0(sp)
	mv a2, fp
	li a3, 8
	mv a3, a3
	add a2, a2, a3
	mv a2, a2
	mv a4, a2
	mv a3, fp
	li a2, 8
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	ld a2, 0(a2)
	negw a2, a2
	sd a2, 0(a4)
	j L8
L7:
L8:
	mv a3, fp
	li a2, 8
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	ld a2, 0(a2)
	mv a2, a2
	li a3, 10
	mv a3, a3
	div a2, a2, a3
	mv a2, a2
	mv a3, a2
	li a2, 0
	mv a2, a2
	sub a2, a3, a2
	snez a2, a2
	mv a2, a2
	bnez a2, L9
L21:
	j L10
L9:
	mv a3, fp
	li a2, 8
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	ld a2, 0(a2)
	mv a3, a2
	li a2, 10
	mv a2, a2
	div a2, a3, a2
	mv a2, a2
	mv a2, a2
	li a3, 0
	sd a3, 0(sp)
	sd a2, 8(sp)
	call _putint
	ld a2, 0(sp)
	j L11
L10:
L11:
	mv a2, fp
	li a3, 8
	mv a3, a3
	add a2, a2, a3
	mv a2, a2
	ld a2, 0(a2)
	mv a3, a2
	li a2, 10
	mv a2, a2
	rem a2, a3, a2
	mv a2, a2
	mv a3, a2
	li a2, 48
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	mv a2, a2
	li a3, 256
	mv a3, a3
	rem a2, a2, a3
	mv a2, a2
	mv a2, a2
	li a3, 0
	sd a3, 0(sp)
	sd a2, 8(sp)
	call _putchar
	ld a2, 0(sp)
	li a2, 0
	mv a2, a2
L5:
	j L19
L19:
	ld a4, 48(fp)
	ld a3, 40(fp)
	ld a2, 32(fp)
	ld ra, 24(fp)
	mv sp, fp
	ld fp, 16(sp)
	ret
