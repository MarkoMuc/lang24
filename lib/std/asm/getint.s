.section .text
.global _getint
_getint:
	sd fp, 32(sp)
	mv fp, sp
	sd ra, 40(fp)
	sd a2, 48(fp)
	sd a3, 56(fp)
	sd a4, 64(fp)
	sd a5, 72(fp)
	add sp, fp, 72
	j L23
L23:
L8:
	mv a3, fp
	li a2, -16
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	mv a2, a2
	call _getchar
	ld a3, 0(sp)
	mv a3, a3
	sd a3, 0(a2)
	mv a3, fp
	li a2, -8
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	mv a2, a2
	li a3, 0
	sd a3, 0(a2)
L10:
	mv a3, fp
	li a2, -16
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	ld a2, 0(a2)
	mv a3, a2
	li a2, 48
	mv a2, a2
	slt a2, a3, a2
	seqz a2, a2
	mv a2, a2
	mv a4, a2
	mv a2, fp
	li a3, -16
	mv a3, a3
	add a2, a2, a3
	mv a2, a2
	ld a2, 0(a2)
	mv a2, a2
	li a3, 57
	mv a3, a3
	slt a2, a3, a2
	seqz a2, a2
	mv a2, a2
	mv a2, a2
	and a2, a4, a2
	mv a2, a2
	bnez a2, L11
L25:
	j L12
L11:
	mv a2, fp
	li a3, -8
	mv a3, a3
	add a2, a2, a3
	mv a2, a2
	mv a3, a2
	mv a2, fp
	li a4, -8
	mv a4, a4
	add a2, a2, a4
	mv a2, a2
	ld a2, 0(a2)
	mv a4, a2
	li a2, 10
	mv a2, a2
	mul a2, a4, a2
	mv a2, a2
	sd a2, 0(a3)
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
	mv a5, a2
	mv a2, fp
	li a3, -16
	mv a3, a3
	add a2, a2, a3
	mv a2, a2
	ld a2, 0(a2)
	mv a2, a2
	li a3, 48
	mv a3, a3
	sub a2, a2, a3
	mv a2, a2
	mv a2, a2
	add a2, a5, a2
	mv a2, a2
	sd a2, 0(a4)
	mv a3, fp
	li a2, -16
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	mv a3, a2
	call _getchar
	ld a2, 0(sp)
	mv a2, a2
	sd a2, 0(a3)
	j L10
L12:
	mv a4, fp
	mv a3, fp
	li a2, -8
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	ld a2, 0(a2)
	sd a2, 0(a4)
	j L9
L9:
	j L24
L24:
	ld a5, 72(fp)
	ld a4, 64(fp)
	ld a3, 56(fp)
	ld a2, 48(fp)
	ld ra, 40(fp)
	mv sp, fp
	ld fp, 32(sp)
	ret
