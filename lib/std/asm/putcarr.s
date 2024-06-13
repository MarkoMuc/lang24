# putcarr(SL, *char, len)
.section .text
.global _putcarr
_putcarr:
	addi sp, sp, -16
	sd fp, 0(sp)
	mv fp, sp
	addi fp, fp, 16
	addi sp, sp, -8
	sd ra, 0(sp)
	addi sp, sp, -8
	sd a2, 0(sp)
	addi sp, sp, -8
	sd a3, 0(sp)
	addi sp, sp, -8
	sd a4, 0(sp)
	addi sp, sp, -16
	j L15
L15:
L4:
	mv a3, fp
	li a2, -8
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	mv a2, a2
	li a3, 0
	sd a3, 0(a2)
L6:
	mv a2, fp
	li a3, -8
	mv a3, a3
	add a2, a2, a3
	mv a2, a2
	ld a2, 0(a2)
	mv a4, a2
	mv a3, fp
	li a2, 16
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	ld a2, 0(a2)
	mv a2, a2
	slt a2, a4, a2
	mv a2, a2
	bnez a2, L7
L17:
	j L8
L7:
	mv a3, fp
	li a2, 8
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	ld a2, 0(a2)
	mv a4, a2
	mv a2, fp
	li a3, -8
	mv a3, a3
	add a2, a2, a3
	mv a2, a2
	ld a2, 0(a2)
	mv a2, a2
	li a3, 8
	mv a3, a3
	mul a2, a2, a3
	mv a2, a2
	mv a2, a2
	add a2, a4, a2
	mv a2, a2
	ld a2, 0(a2)
	mv a3, a2
	li a2, 0
	sd a2, 0(sp)
	sd a3, 8(sp)
	call _putchar
	ld a2, 0(sp)
	mv a3, fp
	li a2, -8
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	mv a4, a2
	mv a3, fp
	li a2, -8
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	ld a2, 0(a2)
	mv a3, a2
	li a2, 1
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	sd a2, 0(a4)
	j L6
L8:
	li a2, 0
	mv a2, a2
L5:
	j L16
L16:
	addi sp, sp, 16
	ld a4, 0(sp)
	addi sp, sp, 8
	ld a3, 0(sp)
	addi sp, sp, 8
	ld a2, 0(sp)
	addi sp, sp, 8
	ld ra, 0(sp)
	addi sp, sp, 8
	ld fp, 0(sp)
	addi sp, sp, 16
	ret
